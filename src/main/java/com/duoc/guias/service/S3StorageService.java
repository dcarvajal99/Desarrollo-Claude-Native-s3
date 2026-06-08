package com.duoc.guias.service;

import com.duoc.guias.exception.StorageException;
import com.duoc.guias.model.Guia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * Almacenamiento definitivo en AWS S3.
 *
 * Las guias se organizan en carpetas por fecha y transportista, segun el caso:
 *   /{fecha}/{transportista}/guia{numero}.pdf   ej: /20211/transportistaX/guia123.pdf
 *
 * Aqui la "fecha" es la fecha de despacho en formato yyyyMMdd para que cada dia tenga
 * su carpeta. El transportista se normaliza (sin espacios) para que sea una key valida.
 */
@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);
    private static final DateTimeFormatter CARPETA_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final S3Client s3Client;
    private final String bucket;

    public S3StorageService(S3Client s3Client, @Value("${app.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    /**
     * Construye la key de S3 organizada por fecha y transportista.
     * Ej: 20211231/transportistax/guia123.pdf
     */
    public String construirKey(Guia guia) {
        String fecha = guia.getFechaDespacho().format(CARPETA_FECHA);
        String transportista = normalizar(guia.getTransportista());
        return fecha + "/" + transportista + "/guia" + guia.getNumeroGuia() + ".pdf";
    }

    /** Sube (o sobreescribe) el PDF de la guia a S3. Devuelve la key utilizada. */
    public String subir(Guia guia, Path archivoLocal) {
        String key = construirKey(guia);
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/pdf")
                    .build();
            s3Client.putObject(req, RequestBody.fromFile(archivoLocal));
            log.info("Guia {} subida a S3: s3://{}/{}", guia.getNumeroGuia(), bucket, key);
            return key;
        } catch (S3Exception ex) {
            throw new StorageException("No se pudo subir la guia a S3 (key=" + key + "): "
                    + ex.awsErrorDetails().errorMessage(), ex);
        }
    }

    /** Descarga el contenido del PDF desde S3. */
    public byte[] descargar(String key) {
        try {
            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(req);
            log.info("Guia descargada de S3: s3://{}/{}", bucket, key);
            return bytes.asByteArray();
        } catch (NoSuchKeyException ex) {
            throw new StorageException("La guia no existe en S3 (key=" + key + ")", ex);
        } catch (S3Exception ex) {
            throw new StorageException("No se pudo descargar la guia desde S3 (key=" + key + "): "
                    + ex.awsErrorDetails().errorMessage(), ex);
        }
    }

    /** Elimina el objeto de S3 (best-effort). */
    public void eliminar(String key) {
        if (key == null || key.isBlank()) return;
        try {
            DeleteObjectRequest req = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(req);
            log.info("Guia eliminada de S3: s3://{}/{}", bucket, key);
        } catch (S3Exception ex) {
            throw new StorageException("No se pudo eliminar la guia de S3 (key=" + key + "): "
                    + ex.awsErrorDetails().errorMessage(), ex);
        }
    }

    public String getBucket() {
        return bucket;
    }

    // Normaliza el transportista para usarlo como segmento de key (minusculas, sin espacios)
    private String normalizar(String valor) {
        return valor.trim().toLowerCase().replaceAll("\\s+", "");
    }
}
