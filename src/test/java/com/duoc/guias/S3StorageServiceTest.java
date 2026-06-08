package com.duoc.guias;

import com.duoc.guias.exception.StorageException;
import com.duoc.guias.model.Guia;
import com.duoc.guias.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del almacenamiento en AWS S3. El S3Client real se reemplaza por un mock
 * (Mockito) para no necesitar credenciales ni red; se verifica la organizacion de
 * carpetas por fecha/transportista y las operaciones subir/descargar/eliminar.
 */
class S3StorageServiceTest {

    private S3Client s3Client;
    private S3StorageService service;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        service = new S3StorageService(s3Client, "bucket-test");
    }

    private Guia nuevaGuia() {
        return new Guia(1L, "123", "Transportista X", "Cliente Demo",
                "Av. Siempre Viva 742", "Caja de prueba",
                LocalDate.of(2021, 1, 1), null, null, false, LocalDateTime.now());
    }

    @Test
    void construyeKeyOrganizadaPorFechaYTransportista() {
        // /20210101/transportistax/guia123.pdf  (fecha sin guiones, transportista normalizado)
        assertEquals("20210101/transportistax/guia123.pdf", service.construirKey(nuevaGuia()));
    }

    @Test
    void subeAlBucketConLaKeyCorrecta(@TempDir Path tmp) throws IOException {
        Path archivo = Files.write(tmp.resolve("guia123.pdf"), "pdf".getBytes());

        String key = service.subir(nuevaGuia(), archivo);

        assertEquals("20210101/transportistax/guia123.pdf", key);
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals("bucket-test", captor.getValue().bucket());
        assertEquals(key, captor.getValue().key());
        assertEquals("application/pdf", captor.getValue().contentType());
    }

    @Test
    void descargaDevuelveElContenidoDelObjeto() {
        byte[] esperado = "contenido-pdf".getBytes();
        ResponseBytes<GetObjectResponse> respuesta =
                ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), esperado);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(respuesta);

        byte[] actual = service.descargar("20210101/transportistax/guia123.pdf");

        assertArrayEquals(esperado, actual);
    }

    @Test
    void descargaDeKeyInexistenteLanzaStorageException() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("no existe").build());

        assertThrows(StorageException.class, () -> service.descargar("no/existe.pdf"));
    }

    @Test
    void eliminaElObjetoDelBucket() {
        service.eliminar("20210101/transportistax/guia123.pdf");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("bucket-test", captor.getValue().bucket());
        assertEquals("20210101/transportistax/guia123.pdf", captor.getValue().key());
    }
}
