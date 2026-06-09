package com.duoc.guias.controller;

import com.duoc.guias.dto.ApiResponse;
import com.duoc.guias.dto.LoginRequest;
import com.duoc.guias.dto.LoginResponse;
import com.duoc.guias.dto.RegisterRequest;
import com.duoc.guias.dto.TransportistaDTO;
import com.duoc.guias.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacion", description = "Registro e inicio de sesion de transportistas (devuelve token JWT)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Registrar transportista",
            description = "Crea un transportista nuevo con su clave encriptada (BCrypt).")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TransportistaDTO>> register(@Valid @RequestBody RegisterRequest req) {
        TransportistaDTO created = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Transportista registrado correctamente", created));
    }

    @Operation(summary = "Iniciar sesion (login)",
            description = "Valida usuario y clave; devuelve el token JWT para usar en Authorization: Bearer <token>.")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok("Login exitoso", authService.login(req));
    }
}
