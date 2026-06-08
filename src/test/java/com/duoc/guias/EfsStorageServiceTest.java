package com.duoc.guias;

import com.duoc.guias.model.Guia;
import com.duoc.guias.service.EfsStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del almacenamiento temporal EFS: el PDF se genera de verdad en el disco,
 * se puede leer, y se elimina. Usa un @TempDir que simula el punto de montaje EFS.
 */
class EfsStorageServiceTest {

    private Guia nuevaGuia() {
        return new Guia(1L, "123", "TransportistaX", "Cliente Demo",
                "Av. Siempre Viva 742", "Caja de prueba",
                LocalDate.of(2025, 6, 1), null, null, false, LocalDateTime.now());
    }

    @Test
    void generaPdfRealEnElEfs(@TempDir Path efs) throws Exception {
        EfsStorageService service = new EfsStorageService(efs.toString());
        Guia guia = nuevaGuia();

        Path archivo = service.generarPdf(guia);

        assertTrue(Files.exists(archivo), "el PDF debe existir en el EFS");
        assertEquals("guia123.pdf", archivo.getFileName().toString());

        byte[] contenido = Files.readAllBytes(archivo);
        assertTrue(contenido.length > 0, "el PDF no debe estar vacio");
        // Firma de un archivo PDF: empieza con %PDF
        assertEquals('%', contenido[0]);
        assertEquals('P', contenido[1]);
        assertEquals('D', contenido[2]);
        assertEquals('F', contenido[3]);
    }

    @Test
    void leeYEliminaElPdf(@TempDir Path efs) {
        EfsStorageService service = new EfsStorageService(efs.toString());
        Guia guia = nuevaGuia();

        Path archivo = service.generarPdf(guia);
        byte[] viaServicio = service.leerPdf(archivo);
        assertTrue(viaServicio.length > 0);

        service.eliminarPdf(archivo);
        assertFalse(Files.exists(archivo), "el PDF debe ser eliminado del EFS");
    }

    @Test
    void leerYEscribirCoinciden(@TempDir Path efs) throws Exception {
        EfsStorageService service = new EfsStorageService(efs.toString());
        Guia guia = nuevaGuia();

        Path archivo = service.generarPdf(guia);
        assertArrayEquals(Files.readAllBytes(archivo), service.leerPdf(archivo));
    }
}
