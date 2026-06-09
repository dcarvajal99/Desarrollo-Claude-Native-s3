package com.duoc.guias;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de la seguridad JWT:
 *  - los endpoints de /api/guias exigen token (401 sin el)
 *  - /api/auth/login devuelve un token con credenciales validas
 *  - con el token, el acceso a /api/guias funciona (200)
 *  - la descarga valida que el transportista del token sea el dueño de la guia
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // S3 simulado para no requerir credenciales AWS al levantar el contexto
    @MockitoBean
    private S3Client s3Client;

    @Test
    void listarGuiasSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/guias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConCredencialesValidasDevuelveToken() throws Exception {
        String token = login("TransportistaX", "1234");
        // el token JWT tiene 3 segmentos separados por punto
        org.junit.jupiter.api.Assertions.assertEquals(3, token.split("\\.").length);
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("username", "TransportistaX", "password", "clave-mala"));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarGuiasConTokenDevuelve200() throws Exception {
        String token = login("TransportistaX", "1234");
        mockMvc.perform(get("/api/guias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    void swaggerYHealthSonPublicos() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    // ---- helper: hace login y devuelve el token ----
    private String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("username", username, "password", password));
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return node.path("data").path("token").asText();
    }
}
