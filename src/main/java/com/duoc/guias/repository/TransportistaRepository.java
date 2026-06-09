package com.duoc.guias.repository;

import com.duoc.guias.model.Transportista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransportistaRepository extends JpaRepository<Transportista, Long> {

    Optional<Transportista> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}
