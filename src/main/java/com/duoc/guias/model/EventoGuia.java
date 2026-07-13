package com.duoc.guias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Tabla NUEVA (Semana 8): bitacora de eventos de guias que el consumidor lee desde
 * la Cola 1 y guarda en la base de datos (PostgreSQL). Es distinta a las tablas de
 * las sumativas anteriores. Cada fila es un evento (CREADA, ACTUALIZADA, ...).
 */
@Entity
@Table(name = "eventos_guia")
public class EventoGuia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operacion", nullable = false, length = 30)
    private String operacion;

    @Column(name = "guia_id")
    private Long guiaId;

    @Column(name = "numero_guia", length = 50)
    private String numeroGuia;

    @Column(name = "transportista", length = 100)
    private String transportista;

    @Column(name = "destinatario", length = 150)
    private String destinatario;

    // Momento en que el consumidor proceso y guardo el evento.
    @Column(name = "fecha_procesado", nullable = false)
    private LocalDateTime fechaProcesado;

    public EventoGuia() { }

    public EventoGuia(Long id, String operacion, Long guiaId, String numeroGuia,
                      String transportista, String destinatario, LocalDateTime fechaProcesado) {
        this.id = id;
        this.operacion = operacion;
        this.guiaId = guiaId;
        this.numeroGuia = numeroGuia;
        this.transportista = transportista;
        this.destinatario = destinatario;
        this.fechaProcesado = fechaProcesado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public LocalDateTime getFechaProcesado() { return fechaProcesado; }
    public void setFechaProcesado(LocalDateTime fechaProcesado) { this.fechaProcesado = fechaProcesado; }
}
