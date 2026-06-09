package com.duoc.guias;

import com.duoc.guias.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del servicio JWT: genera un token firmado, extrae el usuario y valida
 * la firma/expiracion. Usa una clave de prueba de al menos 64 bytes (HS512).
 */
class JwtServiceTest {

    private static final String SECRET =
            "ClaveDePruebaSuperSeguraParaHS512QueTieneMasDeSesentaYCuatroBytesDeLargoTotal123";

    @Test
    void generaTokenYExtraeElUsuario() {
        JwtService jwt = new JwtService(SECRET, 3_600_000L); // 1 hora
        String token = jwt.generateToken("TransportistaX", "TRANSPORTISTA", 1L);

        assertEquals("TransportistaX", jwt.extractUsername(token));
        assertTrue(jwt.isTokenValid(token), "el token recien generado debe ser valido");
    }

    @Test
    void tokenExpiradoNoEsValido() {
        JwtService jwt = new JwtService(SECRET, -1_000L); // ya expirado
        String token = jwt.generateToken("TransportistaX", "TRANSPORTISTA", 1L);
        assertFalse(jwt.isTokenValid(token), "un token expirado no debe ser valido");
    }

    @Test
    void tokenAlteradoNoEsValido() {
        JwtService jwt = new JwtService(SECRET, 3_600_000L);
        String token = jwt.generateToken("TransportistaX", "TRANSPORTISTA", 1L);
        // alterar el ultimo caracter rompe la firma
        String alterado = token.substring(0, token.length() - 1)
                + (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');
        assertFalse(jwt.isTokenValid(alterado), "un token con firma alterada no debe ser valido");
    }
}
