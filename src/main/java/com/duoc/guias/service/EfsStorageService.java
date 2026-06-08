package com.duoc.guias.service;

import com.duoc.guias.exception.StorageException;
import com.duoc.guias.model.Guia;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

/**
 * Almacenamiento temporal en EFS.
 *
 * Genera la guia de despacho como PDF real (OpenPDF) y la escribe en el sistema de
 * archivos montado como EFS. En la EC2 ese disco se monta en /mnt/efs y se inyecta al
 * contenedor con  -v /mnt/efs:/app/efs , por lo que dentro del contenedor la ruta es
 * /app/efs (configurable via app.efs.path / EFS_PATH).
 */
@Service
public class EfsStorageService {

    private static final Logger log = LoggerFactory.getLogger(EfsStorageService.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Path efsRoot;

    public EfsStorageService(@Value("${app.efs.path}") String efsPath) {
        this.efsRoot = Paths.get(efsPath).toAbsolutePath();
    }

    /**
     * Genera el PDF de la guia y lo guarda en el EFS.
     * @return la ruta absoluta del archivo creado dentro del EFS.
     */
    public Path generarPdf(Guia guia) {
        Path destino = efsRoot.resolve(nombreArchivo(guia));
        try {
            Files.createDirectories(efsRoot);
            try (OutputStream out = Files.newOutputStream(destino)) {
                escribirPdf(guia, out);
            }
            log.info("Guia {} generada en EFS: {}", guia.getNumeroGuia(), destino);
            return destino;
        } catch (IOException ex) {
            throw new StorageException("No se pudo escribir la guia en el EFS: " + destino, ex);
        }
    }

    /** Lee el PDF previamente generado en el EFS. */
    public byte[] leerPdf(Path ruta) {
        try {
            return Files.readAllBytes(ruta);
        } catch (IOException ex) {
            throw new StorageException("No se pudo leer la guia desde el EFS: " + ruta, ex);
        }
    }

    /** Elimina el PDF temporal del EFS (best-effort, no falla si no existe). */
    public void eliminarPdf(Path ruta) {
        if (ruta == null) return;
        try {
            boolean borrado = Files.deleteIfExists(ruta);
            if (borrado) log.info("Guia eliminada del EFS: {}", ruta);
        } catch (IOException ex) {
            log.warn("No se pudo eliminar la guia del EFS: {} ({})", ruta, ex.getMessage());
        }
    }

    public String nombreArchivo(Guia guia) {
        return "guia" + guia.getNumeroGuia() + ".pdf";
    }

    public Path getEfsRoot() {
        return efsRoot;
    }

    // ---- Construccion del PDF ----
    private void escribirPdf(Guia guia, OutputStream out) {
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new java.awt.Color(33, 94, 62));
            Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font valor = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph header = new Paragraph("GUIA DE DESPACHO", titulo);
            header.setAlignment(Element.ALIGN_CENTER);
            header.setSpacingAfter(4f);
            doc.add(header);

            Paragraph sub = new Paragraph("Sistema de Gestion de Pedidos - Empresa Transportista", valor);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(16f);
            doc.add(sub);

            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{1.2f, 2.8f});

            fila(tabla, "N° Guia:", guia.getNumeroGuia(), label, valor);
            fila(tabla, "Transportista:", guia.getTransportista(), label, valor);
            fila(tabla, "Destinatario:", guia.getDestinatario(), label, valor);
            fila(tabla, "Direccion:", guia.getDireccion(), label, valor);
            fila(tabla, "Fecha despacho:", guia.getFechaDespacho().format(FECHA), label, valor);
            fila(tabla, "Descripcion:", guia.getDescripcion() != null ? guia.getDescripcion() : "-", label, valor);
            doc.add(tabla);

            Paragraph pie = new Paragraph(
                    "\nDocumento generado automaticamente por el microservicio de guias (CDY2204 - Semana 3).",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, java.awt.Color.GRAY));
            pie.setSpacingBefore(20f);
            doc.add(pie);
        } catch (Exception ex) {
            throw new StorageException("Error al construir el PDF de la guia " + guia.getNumeroGuia(), ex);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    private void fila(PdfPTable tabla, String etiqueta, String contenido, Font label, Font valor) {
        PdfPCell c1 = new PdfPCell(new Phrase(etiqueta, label));
        PdfPCell c2 = new PdfPCell(new Phrase(contenido, valor));
        c1.setPadding(6f);
        c2.setPadding(6f);
        tabla.addCell(c1);
        tabla.addCell(c2);
    }
}
