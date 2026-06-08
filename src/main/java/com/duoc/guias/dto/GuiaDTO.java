package com.duoc.guias.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GuiaDTO {

    private Long id;
    private String numeroGuia;
    private String transportista;
    private String destinatario;
    private String direccion;
    private String descripcion;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaDespacho;

    private String efsPath;
    private String s3Key;
    private boolean subidaS3;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime creadaEn;

    public GuiaDTO() { }

    public GuiaDTO(Long id, String numeroGuia, String transportista, String destinatario,
                   String direccion, String descripcion, LocalDate fechaDespacho,
                   String efsPath, String s3Key, boolean subidaS3, LocalDateTime creadaEn) {
        this.id = id;
        this.numeroGuia = numeroGuia;
        this.transportista = transportista;
        this.destinatario = destinatario;
        this.direccion = direccion;
        this.descripcion = descripcion;
        this.fechaDespacho = fechaDespacho;
        this.efsPath = efsPath;
        this.s3Key = s3Key;
        this.subidaS3 = subidaS3;
        this.creadaEn = creadaEn;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getEfsPath() { return efsPath; }
    public void setEfsPath(String efsPath) { this.efsPath = efsPath; }
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public boolean isSubidaS3() { return subidaS3; }
    public void setSubidaS3(boolean subidaS3) { this.subidaS3 = subidaS3; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
}
