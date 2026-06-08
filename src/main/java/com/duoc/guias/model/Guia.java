package com.duoc.guias.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Guia de despacho. Representa el pedido a despachar y el rastro de su
 * archivo PDF, tanto en el almacenamiento temporal EFS como en AWS S3.
 *
 * La organizacion en S3 sigue el patron pedido por el caso:
 *   /{fecha}/{transportista}/{nombreArchivo}     ej: /20211/transportistaX/guia123.pdf
 * y esa misma key se guarda en s3Key para poder descargar/modificar/eliminar.
 */
@Entity
@Table(name = "guias")
public class Guia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Numero de guia visible al usuario (ej: 123 -> guia123.pdf)
    @Column(name = "numero_guia", nullable = false, unique = true, length = 50)
    private String numeroGuia;

    @Column(name = "transportista", nullable = false, length = 100)
    private String transportista;

    @Column(name = "destinatario", nullable = false, length = 150)
    private String destinatario;

    @Column(name = "direccion", nullable = false, length = 250)
    private String direccion;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    // Fecha de despacho; tambien define la carpeta de organizacion en S3
    @Column(name = "fecha_despacho", nullable = false)
    private LocalDate fechaDespacho;

    // Ruta del PDF generado en el EFS (almacenamiento temporal)
    @Column(name = "efs_path", length = 500)
    private String efsPath;

    // Key del objeto en S3: {fecha}/{transportista}/guia{numero}.pdf
    @Column(name = "s3_key", length = 500)
    private String s3Key;

    // true cuando la guia ya fue subida correctamente a S3
    @Column(name = "subida_s3", nullable = false)
    private boolean subidaS3;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    public Guia() { }

    public Guia(Long id, String numeroGuia, String transportista, String destinatario,
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
