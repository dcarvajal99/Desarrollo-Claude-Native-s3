package com.duoc.guias.config;

import com.duoc.guias.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.List;

/**
 * Seguridad como OAuth2 Resource Server (Semana 5 - Exp 2).
 *
 * El backend ya NO emite tokens propios: ahora valida los JWT (RS256) emitidos por
 * Azure AD (IDaaS). Spring Security descarga las claves publicas del issuer y verifica
 * la firma, la expiracion, el issuer (iss) y la audiencia (aud) de cada request.
 *
 * Abierto: Swagger UI, OpenAPI, actuator/health, info y la consola H2.
 * Protegido: todo lo demas (las guias) exige Authorization: Bearer &lt;token de Azure&gt;.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Servidor de validacion (Azure AD). Se configura en application.properties:
    //   spring.security.oauth2.resourceserver.jwt.issuer-uri=...
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    // Audiencia esperada del token (claim aud = Client ID de la API en Azure).
    // Si se deja vacio, no se valida la audiencia (solo issuer + firma + expiracion).
    @Value("${app.oauth2.audience:}")
    private String audience;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)) // consola H2
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // --- rutas abiertas ---
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                        "/v3/api-docs.yaml", "/swagger-resources/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // --- todo lo demas (las guias) requiere un JWT valido de Azure ---
                .anyRequest().authenticated()
            )
            // Activa la validacion de tokens JWT (filtro BearerTokenAuthenticationFilter)
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            );

        return http.build();
    }

    /**
     * Decoder del JWT con validacion explicita de issuer (siempre) y audiencia (si se
     * configuro app.oauth2.audience). El issuer-uri lo lee Spring del properties para
     * descubrir las claves publicas de Azure (.well-known/openid-configuration).
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        if (audience != null && !audience.isBlank()) {
            OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                    JwtClaimNames.AUD,
                    aud -> aud != null && aud.contains(audience));
            decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                    withIssuer, audienceValidator));
        } else {
            decoder.setJwtValidator(withIssuer);
        }
        return decoder;
    }

    // Peticion sin token (o token invalido) a un recurso protegido -> 401 con ErrorResponse JSON
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        ObjectMapper mapper = jsonMapper();
        return (request, response, authException) -> {
            ErrorResponse body = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    "Se requiere un token JWT valido emitido por Azure AD para acceder a este recurso",
                    request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            mapper.writeValue(response.getWriter(), body);
        };
    }

    // Autenticado pero sin permiso sobre el recurso -> 403
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper mapper = jsonMapper();
        return (request, response, accessDeniedException) -> {
            ErrorResponse body = new ErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    HttpStatus.FORBIDDEN.getReasonPhrase(),
                    "No tiene permisos para acceder a este recurso",
                    request.getRequestURI());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            mapper.writeValue(response.getWriter(), body);
        };
    }

    private ObjectMapper jsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
