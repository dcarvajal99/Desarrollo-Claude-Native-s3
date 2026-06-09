package com.duoc.guias.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentacion OpenAPI / Swagger UI.
 * La interfaz queda disponible en /swagger-ui.html y el JSON en /v3/api-docs.
 * Define el esquema de seguridad "bearerAuth" para que aparezca el boton Authorize
 * donde se pega el token JWT obtenido en /api/auth/login.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI guiasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API - Sistema de Guias de Despacho")
                        .version("1.0.0")
                        .description("Microservicio Cloud Native para la gestion de guias de despacho. "
                                + "Las guias se generan como PDF y se almacenan temporalmente en EFS y "
                                + "definitivamente en AWS S3 (organizadas por fecha y transportista). "
                                + "Los endpoints de /api/guias requieren un token JWT: primero haz login en "
                                + "/api/auth/login, copia el token y pegalo en el boton Authorize. "
                                + "Semana 3 - Desarrollo Cloud Native (CDY2204), Duoc UC.")
                        .contact(new Contact().name("Desarrollo Cloud Native - CDY2204"))
                        .license(new License().name("Uso academico - Duoc UC")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Pega aqui el token devuelto por /api/auth/login (sin la palabra Bearer).")));
    }
}
