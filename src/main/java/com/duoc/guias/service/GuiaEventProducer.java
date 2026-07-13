package com.duoc.guias.service;

import com.duoc.guias.dto.EventoGuiaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Productor (Semana 8): publica los eventos de las guias en la Cola 1.
 * Cada operacion del sistema de guias (crear, actualizar, eliminar, subir a S3)
 * genera un evento que se envia a la cola como JSON.
 */
@Service
public class GuiaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(GuiaEventProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final String queuePrincipal;

    public GuiaEventProducer(RabbitTemplate rabbitTemplate,
                             @Value("${app.rabbitmq.queue-principal}") String queuePrincipal) {
        this.rabbitTemplate = rabbitTemplate;
        this.queuePrincipal = queuePrincipal;
    }

    /** Publica un evento de guia en la Cola 1. */
    public void publicar(EventoGuiaDTO evento) {
        rabbitTemplate.convertAndSend(queuePrincipal, evento);
        log.info("Evento '{}' de la guia {} enviado a la Cola 1 '{}' (forzarError={})",
                evento.getOperacion(), evento.getNumeroGuia(), queuePrincipal, evento.isForzarError());
    }
}
