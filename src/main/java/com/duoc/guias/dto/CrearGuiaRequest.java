package com.duoc.guias.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos para crear una guia de despacho. Si no se envia numeroGuia el servicio
 * genera uno automaticamente. La fechaDespacho organiza la carpeta en S3.
 */
public class CrearGuiaRequest {

    // Opcional: si viene vacio el servicio asigna un correlativo
    @Size(max = 50, message = "numeroGuia must be at most 50 characters")
    private String numeroGuia;

    @NotBlank(message = "transportista is required")
    @Size(min = 2, max = 100, message = "transportista must be between 2 and 100 characters")
    private String transportista;

    @NotBlank(message = "destinatario is required")
    @Size(min = 2, max = 150, message = "destinatario must be between 2 and 150 characters")
    private String destinatario;

    @NotBlank(message = "direccion is required")
    @Size(min = 2, max = 250, message = "direccion must be between 2 and 250 characters")
    private String direccion;

    @Size(max = 500, message = "descripcion must be at most 500 characters")
    private String descripcion;

    @NotNull(message = "fechaDespacho is required (formato yyyy-MM-dd)")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaDespacho;

    // Semana 8 (opcional): si es true, el evento se marca para que el consumidor lo
    // rechace y RabbitMQ lo envie a la Cola 2 (dead-letter). Solo para demostrar el flujo.
    private boolean forzarError;

    public CrearGuiaRequest() { }

    public CrearGuiaRequest(String numeroGuia, String transportista, String destinatario,
                            String direccion, String descripcion, LocalDate fechaDespacho) {
        this.numeroGuia = numeroGuia;
        this.transportista = transportista;
        this.destinatario = destinatario;
        this.direccion = direccion;
        this.descripcion = descripcion;
        this.fechaDespacho = fechaDespacho;
    }

    public String getNumeroGuia() { return numeroGuia; }
    public void setNumeroGuia(String numeroGuia) { this.numeroGuia = numeroGuia; }
    public String getTransportista() { return transportista; }
    public void setTransportista(String transportista) { this.transportista = transportista; }
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDate getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(LocalDate fechaDespacho) { this.fechaDespacho = fechaDespacho; }
    public boolean isForzarError() { return forzarError; }
    public void setForzarError(boolean forzarError) { this.forzarError = forzarError; }
}
