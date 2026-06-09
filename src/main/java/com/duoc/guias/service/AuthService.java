package com.duoc.guias.service;

import com.duoc.guias.dto.EntityMapper;
import com.duoc.guias.dto.LoginRequest;
import com.duoc.guias.dto.LoginResponse;
import com.duoc.guias.dto.RegisterRequest;
import com.duoc.guias.dto.TransportistaDTO;
import com.duoc.guias.exception.StorageException;
import com.duoc.guias.exception.UnauthorizedException;
import com.duoc.guias.model.Transportista;
import com.duoc.guias.repository.TransportistaRepository;
import com.duoc.guias.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final TransportistaRepository transportistaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EntityMapper mapper;

    public AuthService(TransportistaRepository transportistaRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EntityMapper mapper) {
        this.transportistaRepository = transportistaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    // Registro de un nuevo transportista (clave encriptada con BCrypt)
    public TransportistaDTO register(RegisterRequest req) {
        if (transportistaRepository.existsByUsernameIgnoreCase(req.getUsername())) {
            throw new StorageException("Username already registered: " + req.getUsername());
        }
        Transportista t = new Transportista();
        t.setUsername(req.getUsername());
        t.setPassword(passwordEncoder.encode(req.getPassword()));
        t.setRole("TRANSPORTISTA");
        return mapper.toDTO(transportistaRepository.save(t));
    }

    // Login: valida credenciales y devuelve el token JWT
    public LoginResponse login(LoginRequest req) {
        Transportista t = transportistaRepository.findByUsernameIgnoreCase(req.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Usuario o contraseña invalidos"));

        if (!passwordEncoder.matches(req.getPassword(), t.getPassword())) {
            throw new UnauthorizedException("Usuario o contraseña invalidos");
        }

        String token = jwtService.generateToken(t.getUsername(), t.getRole(), t.getId());
        return new LoginResponse(token, jwtService.getExpirationMs() / 1000, mapper.toDTO(t));
    }
}
