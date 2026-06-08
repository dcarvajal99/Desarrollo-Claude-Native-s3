package com.duoc.guias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Construye el cliente de AWS S3.
 *
 * Las credenciales se resuelven con DefaultCredentialsProvider, que lee (en orden)
 * variables de entorno, propiedades del sistema, el archivo ~/.aws/credentials y el
 * instance profile de la EC2. Esto es compatible con las credenciales temporales de
 * AWS Academy (incluido el AWS_SESSION_TOKEN) sin necesidad de escribir secretos en
 * el codigo ni en application.properties.
 */
@Configuration
public class S3Config {

    @Value("${app.s3.region}")
    private String region;

    // Endpoint opcional: vacio = AWS real. Se usa para apuntar a un S3 simulado en tests.
    @Value("${app.s3.endpoint:}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (endpoint != null && !endpoint.isBlank()) {
            // path-style + endpoint custom: necesario para emuladores tipo S3Mock/LocalStack
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }

        return builder.build();
    }
}
