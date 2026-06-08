package com.duoc.guias.repository;

import com.duoc.guias.model.Guia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GuiaRepository extends JpaRepository<Guia, Long> {

    Optional<Guia> findByNumeroGuia(String numeroGuia);

    boolean existsByNumeroGuia(String numeroGuia);

    // Consulta: guias por transportista y fecha (criterio 5 de la pauta)
    List<Guia> findByTransportistaAndFechaDespacho(String transportista, LocalDate fechaDespacho);

    // Filtros parciales para la consulta de historial
    List<Guia> findByTransportista(String transportista);

    List<Guia> findByFechaDespacho(LocalDate fechaDespacho);
}
