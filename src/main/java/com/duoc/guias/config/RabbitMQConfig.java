package com.duoc.guias.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de RabbitMQ con DOS colas y patron Dead Letter (Semana 8).
 *
 *  - COLA 1 (principal): "guias.eventos.queue". Recibe los eventos de las guias.
 *    Se declara con un x-dead-letter-exchange: si un mensaje es RECHAZADO por el
 *    consumidor (lanza excepcion y no se re-encola), RabbitMQ lo envia al DLX.
 *
 *  - EXCHANGE dead-letter: "guias.dlx" (DirectExchange).
 *
 *  - COLA 2 (errores): "guias.eventos.error.queue". Ligada al DLX por una routing key.
 *    Aqui quedan almacenados todos los mensajes que fallaron.
 *
 * Flujo: productor -> Cola 1 -> consumidor
 *            (ok)  -> guarda en la base de datos
 *            (falla) -> rechazo -> DLX -> Cola 2 (errores)
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.queue-principal}")
    private String queuePrincipal;

    @Value("${app.rabbitmq.dlx-exchange}")
    private String dlxExchange;

    @Value("${app.rabbitmq.dlx-routing-key}")
    private String dlxRoutingKey;

    @Value("${app.rabbitmq.queue-errores}")
    private String queueErrores;

    // ---- COLA 1 (principal), con dead-letter hacia el DLX ----
    @Bean
    public Queue guiasQueue() {
        return QueueBuilder.durable(queuePrincipal)
                .withArgument("x-dead-letter-exchange", dlxExchange)
                .withArgument("x-dead-letter-routing-key", dlxRoutingKey)
                .build();
    }

    // ---- Exchange dead-letter ----
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(dlxExchange);
    }

    // ---- COLA 2 (errores / dead-letter) ----
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(queueErrores).build();
    }

    // ---- Binding: liga la cola de errores al DLX con la routing key ----
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with(dlxRoutingKey);
    }

    // ---- Conversor JSON (para enviar/recibir objetos EventoGuiaDTO) ----
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ---- RabbitTemplate (productor) con conversor JSON ----
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    /**
     * Factory de listeners con:
     *  - conversor JSON,
     *  - default-requeue-rejected = false: cuando el consumidor lanza una excepcion,
     *    el mensaje NO se re-encola en la cola 1, sino que se enruta al DLX (cola 2).
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false); // rechazo -> dead letter (no re-encolar)
        return factory;
    }
}
