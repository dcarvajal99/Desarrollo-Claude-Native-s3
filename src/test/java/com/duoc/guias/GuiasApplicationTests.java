package com.duoc.guias;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Verifica que el contexto de Spring levante completo.
 *  - El S3Client real se reemplaza por un mock con @MockitoBean para no requerir AWS.
 *  - El JwtDecoder real se reemplaza por un mock con @MockitoBean para no contactar a
 *    Azure AD al arrancar; el perfil "test" carga un issuer-uri ficticio.
 *  - El ConnectionFactory de RabbitMQ se reemplaza por un mock para que el contexto
 *    arranque sin necesidad de un broker RabbitMQ corriendo (Semana 8).
 */
@SpringBootTest
@ActiveProfiles("test")
class GuiasApplicationTests {

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ConnectionFactory rabbitConnectionFactory;

    @Test
    void contextLoads() {
    }
}
