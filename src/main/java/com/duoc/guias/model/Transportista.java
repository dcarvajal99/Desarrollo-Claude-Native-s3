package com.duoc.guias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Transportista que se autentica en el sistema. El {@code username} es el mismo
 * nombre de transportista que aparece en las guias, de modo que el token JWT
 * identifica al dueño y la descarga puede validar permisos contra el.
 */
@Entity
@Table(name = "transportistas")
public class Transportista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre del transportista (= campo "transportista" de las guias). Sirve de usuario de login.
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    // Clave encriptada con BCrypt (nunca se devuelve en las respuestas)
    @Column(name = "password", nullable = false, length = 200)
    private String password;

    @Column(name = "role", nullable = false, length = 20)
    private String role; // TRANSPORTISTA | ADMIN

    public Transportista() { }

    public Transportista(Long id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
