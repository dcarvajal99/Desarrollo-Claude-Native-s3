package com.duoc.guias.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Mensaje que viaja por la cola (Semana 8): representa un EVENTO ocurrido sobre una
 * guia (creada, actualizada, eliminada, subida a S3...). El productor lo publica en
 * la Cola 1 y el consumidor lo guarda en la base de datos.
 *
 * El campo "forzarError" permite demostrar el flujo de errores: si viene en true, el
 * consumidor rechaza el mensaje y RabbitMQ lo envia a la Cola 2 (dead-letter).
 */
public class EventoGuiaDTO {

    private String operacion;      // CREADA | ACTUALIZADA | ELIMINADA | SUBIDA_S3
    private Long guiaId;
    private String numeroGuia;
    private String transportista;
    private String destinatario;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaEvento;

    private boolean forzarError;   // true = el consumidor lo rechaza (va a la Cola 2)

    public EventoGuiaDTO() { }

    public EventoGuiaDTO(String operacion, Long guiaId, String numeroGuia,
                         String transportista, String destinatario,
                         LocalDateTime fechaEvento, boolean forzarError) {
        this.operacion = operacion;
        this.guiaId = guiaId;
        this.numeroGuia = numeroGuia;
        this.transportista = transportista;
        this.destinatario = destinatario;
        this.fechaEvento = fechaEvento;
        this.forzarError = forzarError;
    }

    // Fabrica: construye el evento a partir de una guia y el tipo de operacion.
    public static EventoGuiaDTO de(String operacion, GuiaDTO guia) {
        return new EventoGuiaDTO(operacion, guia.getId(), guia.getNumeroGuia(),
                guia.getTransportista(), guia.getDestinatario(), LocalDateTime.now(), false);
    }

    public String getOperacion() { return operacion; }
    public void setOperacion(String operacion) { this.operacion = operacion; }
    public Long getGuiaId() { return guiaId; }
    public void setGuiaId(Long guiaId) { this.guiaId = guiaId; }
    public String getNumeroGuia() { return numeroGuia; }
    public void setNumeroGuia(String numeroGuia) { this.numeroGuia = numeroGuia; }
    public String getTransportista() { return transportista; }
    public void setTransportista(String transportista) { this.transportista = transportista; }
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDateTime fechaEvento) { this.fechaEvento = fechaEvento; }
    public boolean isForzarError() { return forzarError; }
    public void setForzarError(boolean forzarError) { this.forzarError = forzarError; }
}
