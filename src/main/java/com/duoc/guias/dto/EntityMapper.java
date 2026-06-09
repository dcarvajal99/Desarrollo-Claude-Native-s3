package com.duoc.guias.dto;

import com.duoc.guias.model.Guia;
import com.duoc.guias.model.Transportista;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EntityMapper {

    // ----- Transportista (usuario) -> nunca expone la password -----
    public TransportistaDTO toDTO(Transportista t) {
        if (t == null) return null;
        return new TransportistaDTO(t.getId(), t.getUsername(), t.getRole());
    }

    public GuiaDTO toDTO(Guia guia) {
        if (guia == null) return null;
        return new GuiaDTO(
                guia.getId(),
                guia.getNumeroGuia(),
                guia.getTransportista(),
                guia.getDestinatario(),
                guia.getDireccion(),
                guia.getDescripcion(),
                guia.getFechaDespacho(),
                guia.getEfsPath(),
                guia.getS3Key(),
                guia.isSubidaS3(),
                guia.getCreadaEn());
    }

    public List<GuiaDTO> toGuiaDTOList(List<Guia> guias) {
        return guias.stream().map(this::toDTO).toList();
    }

    // Construye una nueva entidad a partir del request de creacion.
    // numeroGuia ya viene resuelto desde el servicio (enviado o autogenerado).
    public Guia toEntity(CrearGuiaRequest req, String numeroGuia) {
        return new Guia(
                null,
                numeroGuia,
                req.getTransportista(),
                req.getDestinatario(),
                req.getDireccion(),
                req.getDescripcion(),
                req.getFechaDespacho(),
                null,            // efsPath se llena al generar el PDF
                null,            // s3Key se llena al subir a S3
                false,           // aun no subida
                LocalDateTime.now());
    }
}
