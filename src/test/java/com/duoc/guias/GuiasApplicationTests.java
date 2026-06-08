package com.duoc.guias;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Verifica que el contexto de Spring levante completo. El S3Client real se reemplaza
 * por un mock con @MockitoBean para no requerir credenciales de AWS durante el test.
 */
@SpringBootTest
class GuiasApplicationTests {

    @MockitoBean
    private S3Client s3Client;

    @Test
    void contextLoads() {
    }
}
