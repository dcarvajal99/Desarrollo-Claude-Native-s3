package com.duoc.guias.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentacion OpenAPI / Swagger UI.
 * La interfaz queda disponible en /swagger-ui.html y el JSON en /v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI guiasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API - Sistema de Guias de Despacho")
                        .version("1.0.0")
                        .description("Microservicio Cloud Native para la gestion de guias de despacho. "
                                + "Las guias se generan como PDF y se almacenan temporalmente en EFS y "
                                + "definitivamente en AWS S3 (organizadas por fecha y transportista). "
                                + "Semana 3 - Desarrollo Cloud Native (CDY2204), Duoc UC.")
                        .contact(new Contact().name("Desarrollo Cloud Native - CDY2204"))
                        .license(new License().name("Uso academico - Duoc UC")));
    }
}
