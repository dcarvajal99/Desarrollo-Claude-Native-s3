package com.duoc.guias.repository;

import com.duoc.guias.model.EventoGuia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoGuiaRepository extends JpaRepository<EventoGuia, Long> {
}
