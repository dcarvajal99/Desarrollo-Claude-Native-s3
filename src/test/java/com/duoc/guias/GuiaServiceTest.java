package com.duoc.guias;

import com.duoc.guias.dto.ActualizarGuiaRequest;
import com.duoc.guias.dto.CrearGuiaRequest;
import com.duoc.guias.dto.EntityMapper;
import com.duoc.guias.dto.GuiaDTO;
import com.duoc.guias.exception.ResourceNotFoundException;
import com.duoc.guias.model.Guia;
import com.duoc.guias.repository.GuiaRepository;
import com.duoc.guias.service.EfsStorageService;
import com.duoc.guias.service.GuiaService;
import com.duoc.guias.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de orquestacion del GuiaService: cubre los 6 endpoints a nivel de servicio
 * (crear, subir a S3, descargar con permiso, actualizar, eliminar y consultar) con
 * EFS y S3 simulados mediante mocks.
 */
class GuiaServiceTest {

    private GuiaRepository repo;
    private EfsStorageService efs;
    private S3StorageService s3;
    private GuiaService service;

    @BeforeEach
    void setUp() {
        repo = mock(GuiaRepository.class);
        efs = mock(EfsStorageService.class);
        s3 = mock(S3StorageService.class);
        service = new GuiaService(repo, efs, s3, new EntityMapper());

        // por defecto, save devuelve la misma entidad
        when(repo.save(any(Guia.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CrearGuiaRequest crearReq() {
        return new CrearGuiaRequest("123", "TransportistaX", "Cliente Demo",
                "Av. Demo 100", "Caja", LocalDate.of(2025, 6, 1));
    }

    private Guia guiaPersistida() {
        Guia g = new Guia();
        g.setId(1L);
        g.setNumeroGuia("123");
        g.setTransportista("TransportistaX");
        g.setDestinatario("Cliente Demo");
        g.setDireccion("Av. Demo 100");
        g.setFechaDespacho(LocalDate.of(2025, 6, 1));
        g.setEfsPath("/app/efs/guia123.pdf");
        return g;
    }

    @Test
    void crearGeneraPdfEnEfsYPersiste() {
        when(repo.existsByNumeroGuia("123")).thenReturn(false);
        when(efs.generarPdf(any(Guia.class))).thenReturn(Paths.get("/app/efs/guia123.pdf"));

        GuiaDTO dto = service.crear(crearReq());

        assertEquals("123", dto.getNumeroGuia());
        assertEquals("/app/efs/guia123.pdf", dto.getEfsPath());
        verify(efs).generarPdf(any(Guia.class));
        verify(repo).save(any(Guia.class));
    }

    @Test
    void crearConNumeroDuplicadoFalla() {
        when(repo.existsByNumeroGuia("123")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> service.crear(crearReq()));
        verify(efs, never()).generarPdf(any());
    }

    @Test
    void subirAS3MarcaLaGuiaComoSubida() {
        Guia g = guiaPersistida();
        when(repo.findById(1L)).thenReturn(Optional.of(g));
        when(efs.generarPdf(any())).thenReturn(Paths.get("/app/efs/guia123.pdf"));
        when(s3.subir(any(Guia.class), any(Path.class))).thenReturn("20250601/transportistax/guia123.pdf");

        GuiaDTO dto = service.subirAS3(1L);

        assertTrue(dto.isSubidaS3());
        assertEquals("20250601/transportistax/guia123.pdf", dto.getS3Key());
        verify(s3).subir(any(Guia.class), any(Path.class));
    }

    @Test
    void descargarConTransportistaCorrectoLeeDeS3() {
        Guia g = guiaPersistida();
        g.setSubidaS3(true);
        g.setS3Key("20250601/transportistax/guia123.pdf");
        when(repo.findById(1L)).thenReturn(Optional.of(g));
        when(s3.descargar("20250601/transportistax/guia123.pdf")).thenReturn("pdf".getBytes());

        byte[] contenido = service.descargar(1L, "TransportistaX");

        assertEquals("pdf", new String(contenido));
        verify(s3).descargar(anyString());
    }

    @Test
    void descargarConTransportistaSinPermisoFalla() {
        Guia g = guiaPersistida();
        g.setSubidaS3(true);
        g.setS3Key("k");
        when(repo.findById(1L)).thenReturn(Optional.of(g));

        assertThrows(ResourceNotFoundException.class,
                () -> service.descargar(1L, "OtroTransportista"));
        verify(s3, never()).descargar(anyString());
    }

    @Test
    void actualizarRegeneraPdfYResubeSiEstabaEnS3() {
        Guia g = guiaPersistida();
        g.setSubidaS3(true);
        g.setS3Key("20250601/transportistax/guia123.pdf");
        when(repo.findById(1L)).thenReturn(Optional.of(g));
        when(efs.generarPdf(any())).thenReturn(Paths.get("/app/efs/guia123.pdf"));
        when(s3.subir(any(Guia.class), any(Path.class)))
                .thenReturn("20250705/transportistax/guia123.pdf");

        ActualizarGuiaRequest req = new ActualizarGuiaRequest("TransportistaX", "Nuevo Cliente",
                "Otra Dir 200", "Editada", LocalDate.of(2025, 7, 5));
        GuiaDTO dto = service.actualizar(1L, req);

        assertEquals("Nuevo Cliente", dto.getDestinatario());
        verify(efs).generarPdf(any());
        verify(s3).subir(any(Guia.class), any(Path.class));
        // la key vieja debe borrarse porque cambio la fecha
        verify(s3).eliminar("20250601/transportistax/guia123.pdf");
    }

    @Test
    void eliminarBorraDeS3EfsYBd() {
        Guia g = guiaPersistida();
        g.setS3Key("20250601/transportistax/guia123.pdf");
        when(repo.findById(1L)).thenReturn(Optional.of(g));

        service.eliminar(1L);

        verify(s3).eliminar("20250601/transportistax/guia123.pdf");
        verify(efs).eliminarPdf(any(Path.class));
        verify(repo).delete(g);
    }

    @Test
    void consultarPorTransportistaYFechaUsaElFiltroCombinado() {
        when(repo.findByTransportistaAndFechaDespacho(eq("TransportistaX"), any(LocalDate.class)))
                .thenReturn(List.of(guiaPersistida()));

        List<GuiaDTO> res = service.consultar("TransportistaX", LocalDate.of(2025, 6, 1));

        assertEquals(1, res.size());
        verify(repo).findByTransportistaAndFechaDespacho(eq("TransportistaX"), any(LocalDate.class));
    }

    @Test
    void consultarSinFiltrosDevuelveTodo() {
        when(repo.findAll()).thenReturn(List.of(guiaPersistida(), guiaPersistida()));
        assertEquals(2, service.consultar(null, null).size());
    }
}
