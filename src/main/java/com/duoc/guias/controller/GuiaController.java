package com.duoc.guias.controller;

import com.duoc.guias.dto.ActualizarGuiaRequest;
import com.duoc.guias.dto.ApiResponse;
import com.duoc.guias.dto.CrearGuiaRequest;
import com.duoc.guias.dto.GuiaDTO;
import com.duoc.guias.service.GuiaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    private final GuiaService guiaService;

    public GuiaController(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    // Endpoint 1: crear guia de despacho (genera el PDF en el EFS)
    @PostMapping
    public ResponseEntity<ApiResponse<GuiaDTO>> crear(@Valid @RequestBody CrearGuiaRequest req) {
        GuiaDTO created = guiaService.crear(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Guia de despacho creada y almacenada en EFS", created));
    }

    // Endpoint 2: subir la guia generada a S3
    @PostMapping("/{id}/s3")
    public ApiResponse<GuiaDTO> subirAS3(@PathVariable Long id) {
        return ApiResponse.ok("Guia subida a AWS S3 correctamente", guiaService.subirAS3(id));
    }

    // Endpoint 3: descargar guia con validacion de permisos
    // El transportista solicitante se envia en el header X-Transportista
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id,
                                            @RequestHeader("X-Transportista") String transportista) {
        GuiaDTO guia = guiaService.obtenerDTO(id);
        byte[] contenido = guiaService.descargar(id, transportista);
        String filename = "guia" + guia.getNumeroGuia() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }

    // Endpoint 4: modificar / actualizar guia (regenera PDF y re-sube a S3)
    @PutMapping("/{id}")
    public ApiResponse<GuiaDTO> actualizar(@PathVariable Long id,
                                           @Valid @RequestBody ActualizarGuiaRequest req) {
        return ApiResponse.ok("Guia actualizada correctamente", guiaService.actualizar(id, req));
    }

    // Endpoint 5: eliminar guia especifica (de S3, EFS y BD)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        guiaService.eliminar(id);
        return ApiResponse.ok("Guia eliminada correctamente", null);
    }

    // Endpoint 6: consultar guias por transportista y fecha (historial)
    @GetMapping
    public ApiResponse<List<GuiaDTO>> consultar(
            @RequestParam(required = false) String transportista,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<GuiaDTO> guias = guiaService.consultar(transportista, fecha);
        return ApiResponse.ok("Listado de guias recuperado correctamente", guias);
    }

    // Detalle individual de una guia (apoyo a la demo)
    @GetMapping("/{id}")
    public ApiResponse<GuiaDTO> obtener(@PathVariable Long id) {
        return ApiResponse.ok("Guia recuperada correctamente", guiaService.obtenerDTO(id));
    }
}
