package com.duoc.guias.controller;

import com.duoc.guias.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Control del consumidor de la Cola 1 (Semana 8, apoyo a la demo).
 *
 * Permite PAUSAR el consumidor para que los mensajes se ACUMULEN en la Cola 1
 * (visibles en la consola de RabbitMQ), y luego REANUDARLO para que se procesen.
 * Asi se puede demostrar el comportamiento de la cola sin que los mensajes
 * "pasen demasiado rapido".
 */
@RestController
@RequestMapping("/api/consumidor")
@Tag(name = "Control del consumidor (colas)", description = "Pausar/reanudar el consumidor de la Cola 1 para la demo. Requiere token JWT.")
@SecurityRequirement(name = "bearerAuth")
public class ConsumidorController {

    private static final String LISTENER_ID = "consumidorCola1";

    private final RabbitListenerEndpointRegistry registry;

    public ConsumidorController(RabbitListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    @Operation(summary = "Pausar el consumidor de la Cola 1",
            description = "Detiene el consumo: los mensajes que lleguen se acumulan en la cola (visibles en RabbitMQ).")
    @PostMapping("/pausar")
    public ApiResponse<Map<String, Object>> pausar() {
        container().stop();
        return ApiResponse.ok("Consumidor de la Cola 1 PAUSADO. Los mensajes se acumularan en la cola.", estado());
    }

    @Operation(summary = "Reanudar el consumidor de la Cola 1",
            description = "Reanuda el consumo: procesa los mensajes acumulados en la cola.")
    @PostMapping("/reanudar")
    public ApiResponse<Map<String, Object>> reanudar() {
        container().start();
        return ApiResponse.ok("Consumidor de la Cola 1 REANUDADO. Se procesaran los mensajes acumulados.", estado());
    }

    @Operation(summary = "Estado del consumidor de la Cola 1")
    @GetMapping("/estado")
    public ApiResponse<Map<String, Object>> estadoActual() {
        return ApiResponse.ok("Estado del consumidor", estado());
    }

    private MessageListenerContainer container() {
        MessageListenerContainer c = registry.getListenerContainer(LISTENER_ID);
        if (c == null) {
            throw new IllegalStateException("No se encontro el listener '" + LISTENER_ID + "'");
        }
        return c;
    }

    private Map<String, Object> estado() {
        Map<String, Object> m = new HashMap<>();
        m.put("listener", LISTENER_ID);
        m.put("activo", container().isRunning());
        return m;
    }
}
