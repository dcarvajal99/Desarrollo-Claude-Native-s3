package com.duoc.guias.controller;

import com.duoc.guias.dto.ActualizarGuiaRequest;
import com.duoc.guias.dto.ApiResponse;
import com.duoc.guias.dto.CrearGuiaRequest;
import com.duoc.guias.dto.GuiaDTO;
import com.duoc.guias.service.GuiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/guias")
@Tag(name = "Guias de Despacho", description = "Crear, subir a S3, descargar, actualizar, eliminar y consultar guias de despacho. Requiere token JWT.")
@SecurityRequirement(name = "bearerAuth")
public class GuiaController {

    private final GuiaService guiaService;

    // Claim del token de Azure B2C que identifica al transportista dueño (Semana 6).
    // Se crea en Azure como custom attribute "nombreTransportista" -> extension_nombreTransportista.
    @org.springframework.beans.factory.annotation.Value("${app.oauth2.transportista-claim:extension_nombreTransportista}")
    private String transportistaClaim;

    public GuiaController(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    // Endpoint 1: crear guia de despacho (genera el PDF en el EFS)
    @Operation(summary = "1. Crear guia de despacho",
            description = "Crea la guia y genera su PDF en el almacenamiento temporal EFS.")
    @PostMapping
    public ResponseEntity<ApiResponse<GuiaDTO>> crear(@Valid @RequestBody CrearGuiaRequest req) {
        GuiaDTO created = guiaService.crear(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Guia de despacho creada y almacenada en EFS", created));
    }

    // Endpoint 2: subir la guia generada a S3
    @Operation(summary = "2. Subir guia a AWS S3",
            description = "Sube el PDF de la guia a S3 en la carpeta {yyyyMMdd}/{transportista}/guia{numero}.pdf.")
    @PostMapping("/{id}/s3")
    public ApiResponse<GuiaDTO> subirAS3(@PathVariable Long id) {
        return ApiResponse.ok("Guia subida a AWS S3 correctamente", guiaService.subirAS3(id));
    }

    // Endpoint 3: descargar guia con validacion de permisos
    // El transportista solicitante se obtiene del custom claim "extension_nombreTransportista"
    // del token de Azure B2C. Si no viene, cae al "sub" como respaldo.
    @Operation(summary = "3. Descargar guia (con validacion de permisos)",
            description = "Descarga el PDF. Solo el transportista dueño (identificado por el claim de nombre de su token de Azure B2C) puede hacerlo. Requiere rol 'descarga'.")
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id,
                                            @AuthenticationPrincipal Jwt jwt) {
        // nombre del transportista desde el custom claim; respaldo al sub si no existe
        String transportista = jwt.getClaimAsString(transportistaClaim);
        if (transportista == null || transportista.isBlank()) {
            transportista = jwt.getSubject();
        }
        GuiaDTO guia = guiaService.obtenerDTO(id);
        byte[] contenido = guiaService.descargar(id, transportista);
        String filename = "guia" + guia.getNumeroGuia() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }

    // Endpoint 4: modificar / actualizar guia (regenera PDF y re-sube a S3)
    @Operation(summary = "4. Modificar / actualizar guia",
            description = "Actualiza los datos, regenera el PDF en EFS y lo re-sube a S3 (si ya estaba subida).")
    @PutMapping("/{id}")
    public ApiResponse<GuiaDTO> actualizar(@PathVariable Long id,
                                           @Valid @RequestBody ActualizarGuiaRequest req) {
        return ApiResponse.ok("Guia actualizada correctamente", guiaService.actualizar(id, req));
    }

    // Endpoint 5: eliminar guia especifica (de S3, EFS y BD)
    @Operation(summary = "5. Eliminar guia",
            description = "Elimina la guia de AWS S3, del EFS y de la base de datos.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        guiaService.eliminar(id);
        return ApiResponse.ok("Guia eliminada correctamente", null);
    }

    // Endpoint 6: consultar guias por transportista y fecha (historial)
    @Operation(summary = "6. Consultar guias por transportista y fecha",
            description = "Lista las guias. Filtra por transportista y/o fecha (ambos parametros son opcionales).")
    @GetMapping
    public ApiResponse<List<GuiaDTO>> consultar(
            @RequestParam(required = false) String transportista,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<GuiaDTO> guias = guiaService.consultar(transportista, fecha);
        return ApiResponse.ok("Listado de guias recuperado correctamente", guias);
    }

    // Detalle individual de una guia (apoyo a la demo)
    @Operation(summary = "Detalle de una guia",
            description = "Devuelve los datos de una guia por su id.")
    @GetMapping("/{id}")
    public ApiResponse<GuiaDTO> obtener(@PathVariable Long id) {
        return ApiResponse.ok("Guia recuperada correctamente", guiaService.obtenerDTO(id));
    }
}
