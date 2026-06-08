package com.duoc.guias.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos para modificar/actualizar una guia existente. Al actualizar se regenera
 * el PDF en el EFS y se vuelve a subir a S3 (sobreescribiendo el objeto).
 */
public class ActualizarGuiaRequest {

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

    public ActualizarGuiaRequest() { }

    public ActualizarGuiaRequest(String transportista, String destinatario, String direccion,
                                 String descripcion, LocalDate fechaDespacho) {
        this.transportista = transportista;
        this.destinatario = destinatario;
        this.direccion = direccion;
        this.descripcion = descripcion;
        this.fechaDespacho = fechaDespacho;
    }

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
}
