package com.duoc.guias.service;

import com.duoc.guias.dto.EventoGuiaDTO;
import com.duoc.guias.model.EventoGuia;
import com.duoc.guias.repository.EventoGuiaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Consumidores de las colas (Semana 8).
 *
 *  - COLA 1 (principal): recibe los eventos de guias. Si el evento es valido, lo guarda
 *    en la tabla eventos_guia (PostgreSQL). Si viene marcado con forzarError (o si falla
 *    el guardado), lanza AmqpRejectAndDontRequeueException: RabbitMQ NO lo re-encola y lo
 *    envia al Dead Letter Exchange, que lo deposita en la COLA 2 (errores).
 *
 *  - COLA 2 (errores): consume los mensajes que fallaron, dejando constancia en el log.
 *    (Aqui podria, por ejemplo, notificarse a un administrador.)
 */
@Service
public class GuiaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(GuiaEventConsumer.class);

    private final EventoGuiaRepository eventoRepository;

    public GuiaEventConsumer(EventoGuiaRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    // ---- Consumidor de la COLA 1 ----
    // id="consumidorCola1": permite pausar/reanudar este listener en caliente
    // (ver ConsumidorController), util para demostrar la acumulacion de mensajes en la cola.
    @RabbitListener(id = "consumidorCola1", queues = "${app.rabbitmq.queue-principal}")
    public void consumirEvento(EventoGuiaDTO evento) {
        log.info("Cola 1: evento '{}' recibido (guia {}, forzarError={})",
                evento.getOperacion(), evento.getNumeroGuia(), evento.isForzarError());

        // Demostracion del flujo de error: si el evento pide forzar un fallo, se rechaza
        // y el mensaje termina en la Cola 2.
        if (evento.isForzarError()) {
            log.warn("Cola 1: el evento de la guia {} fue marcado como erroneo -> se envia a la Cola 2",
                    evento.getNumeroGuia());
            throw new AmqpRejectAndDontRequeueException(
                    "Evento marcado como erroneo (forzarError=true) para la guia " + evento.getNumeroGuia());
        }

        // Flujo normal: guardar el evento en la tabla nueva (PostgreSQL).
        try {
            EventoGuia entidad = new EventoGuia(
                    null,
                    evento.getOperacion(),
                    evento.getGuiaId(),
                    evento.getNumeroGuia(),
                    evento.getTransportista(),
                    evento.getDestinatario(),
                    LocalDateTime.now());
            EventoGuia guardado = eventoRepository.save(entidad);
            log.info("Cola 1: evento guardado en la base de datos con id={} (operacion={}, guia={})",
                    guardado.getId(), guardado.getOperacion(), guardado.getNumeroGuia());
        } catch (Exception e) {
            // Cualquier fallo real al persistir tambien manda el mensaje a la Cola 2.
            log.error("Cola 1: error al guardar el evento de la guia {} -> se envia a la Cola 2: {}",
                    evento.getNumeroGuia(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Error al guardar el evento", e);
        }
    }

    // ---- Consumidor de la COLA 2 (errores / dead-letter) ----
    @RabbitListener(queues = "${app.rabbitmq.queue-errores}")
    public void consumirError(EventoGuiaDTO evento) {
        log.error("Cola 2 (ERRORES): mensaje fallido almacenado -> operacion={}, guia={}, transportista={}",
                evento.getOperacion(), evento.getNumeroGuia(), evento.getTransportista());
    }
}
