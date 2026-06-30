package com.duoc.guias.config;

import com.duoc.guias.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Seguridad como OAuth2 Resource Server con AUTORIZACION POR ROLES (Semana 6 - Exp 2).
 *
 * El backend valida los JWT (RS256) emitidos por Azure AD B2C (IDaaS) y, ademas de
 * autenticar, AUTORIZA segun un rol que viaja en un custom claim del token
 * (por defecto "extension_consultaRole").
 *
 * Dos roles (segun el caso de la guia S6):
 *   - "descarga": SOLO puede usar el endpoint de descargar guias.
 *   - "gestion" : el resto de endpoints (crear, subir a S3, actualizar, eliminar, consultar).
 *
 * Abierto: Swagger UI, OpenAPI, actuator/health, info y la consola H2.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // --- Validacion del token ---
    // Issuer (claim iss). En B2C debe coincidir EXACTO con el iss del token (incluido el "/" final).
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    // jwks: URI con las claves publicas para validar la firma. En B2C conviene usarla
    // explicitamente porque el descubrimiento por issuer (.well-known) no siempre funciona.
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    // Audiencia esperada (claim aud = Client ID de la API). Vacio = no se valida.
    @Value("${app.oauth2.audience:}")
    private String audience;

    // --- Roles (custom claim) ---
    // Nombre del claim que trae el rol del usuario en el token de Azure B2C.
    @Value("${app.oauth2.role-claim:extension_consultaRole}")
    private String roleClaim;

    // Valores de los dos roles del caso. Configurables por si cambian en Azure.
    @Value("${app.oauth2.role-descarga:descarga}")
    private String rolDescarga;

    @Value("${app.oauth2.role-gestion:gestion}")
    private String rolGestion;

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

                // --- AUTORIZACION POR ROL (el orden importa: lo mas especifico primero) ---
                // Descargar guia: SOLO el rol "descarga". Debe ir ANTES que la regla de GET /api/guias/{id}.
                .requestMatchers(HttpMethod.GET, "/api/guias/*/descargar").hasRole(rolDescarga)

                // Resto de endpoints de guias: rol "gestion"
                .requestMatchers(HttpMethod.POST, "/api/guias").hasRole(rolGestion)            // crear
                .requestMatchers(HttpMethod.POST, "/api/guias/*/s3").hasRole(rolGestion)        // subir a S3
                .requestMatchers(HttpMethod.PUT, "/api/guias/*").hasRole(rolGestion)            // actualizar
                .requestMatchers(HttpMethod.DELETE, "/api/guias/*").hasRole(rolGestion)         // eliminar
                .requestMatchers(HttpMethod.GET, "/api/guias").hasRole(rolGestion)             // consultar/listar
                .requestMatchers(HttpMethod.GET, "/api/guias/*").hasRole(rolGestion)           // detalle

                // cualquier otra cosa autenticada
                .anyRequest().authenticated()
            )
            // Resource Server: valida el JWT y mapea el rol del claim a una authority de Spring
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            );

        return http.build();
    }

    /**
     * Decoder del JWT. Prioriza jwk-set-uri (recomendado en B2C); si no esta, cae al issuer.
     * Valida issuer (siempre) y audiencia (si se configuro).
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = (jwkSetUri != null && !jwkSetUri.isBlank())
                ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
                : NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        if (audience != null && !audience.isBlank()) {
            OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                    JwtClaimNames.AUD,
                    aud -> aud != null && aud.contains(audience));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));
        } else {
            decoder.setJwtValidator(withIssuer);
        }
        return decoder;
    }

    /**
     * Convierte el rol del custom claim en una authority ROLE_xxx que Spring entiende.
     * Robusto: acepta el claim como String simple ("descarga"), String con varios valores
     * separados por espacio/coma ("descarga gestion"), o como lista JSON.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object raw = jwt.getClaim(roleClaim);
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            if (raw == null) {
                return authorities;
            }
            List<String> roles = new ArrayList<>();
            if (raw instanceof Collection<?> col) {
                for (Object o : col) {
                    if (o != null) roles.add(o.toString());
                }
            } else {
                // String simple o "rol1 rol2" / "rol1,rol2"
                for (String r : raw.toString().split("[ ,]+")) {
                    if (!r.isBlank()) roles.add(r);
                }
            }
            for (String r : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + r.trim()));
            }
            return authorities;
        });
        return converter;
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

    // Autenticado pero sin el ROL requerido sobre el recurso -> 403
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper mapper = jsonMapper();
        return (request, response, accessDeniedException) -> {
            ErrorResponse body = new ErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    HttpStatus.FORBIDDEN.getReasonPhrase(),
                    "Su rol no tiene permiso para acceder a este recurso",
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
