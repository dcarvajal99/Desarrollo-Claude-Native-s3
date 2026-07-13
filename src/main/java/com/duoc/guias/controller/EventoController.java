package com.duoc.guias.controller;

import com.duoc.guias.dto.ApiResponse;
import com.duoc.guias.model.EventoGuia;
import com.duoc.guias.repository.EventoGuiaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint de verificacion (Semana 8): lista los eventos de guias que el consumidor
 * leyó desde la Cola 1 y guardó en la base de datos. Sirve como evidencia de que el
 * flujo asincrono (productor -> Cola 1 -> consumidor -> BD) funcionó correctamente.
 */
@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos de Guias (colas)", description = "Consulta los eventos de guias procesados desde la cola RabbitMQ. Requiere token JWT.")
@SecurityRequirement(name = "bearerAuth")
public class EventoController {

    private final EventoGuiaRepository eventoRepository;

    public EventoController(EventoGuiaRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Operation(summary = "Listar eventos de guias procesados desde la cola",
            description = "Devuelve los eventos que el consumidor leyo de la Cola 1 y guardo en la base de datos.")
    @GetMapping
    public ApiResponse<List<EventoGuia>> listar() {
        return ApiResponse.ok("Eventos recuperados desde la base de datos", eventoRepository.findAll());
    }
}
