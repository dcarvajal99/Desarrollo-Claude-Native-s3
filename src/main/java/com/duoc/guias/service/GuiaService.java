package com.duoc.guias.service;

import com.duoc.guias.dto.ActualizarGuiaRequest;
import com.duoc.guias.dto.CrearGuiaRequest;
import com.duoc.guias.dto.EntityMapper;
import com.duoc.guias.dto.EventoGuiaDTO;
import com.duoc.guias.dto.GuiaDTO;
import com.duoc.guias.exception.ResourceNotFoundException;
import com.duoc.guias.exception.StorageException;
import com.duoc.guias.model.Guia;
import com.duoc.guias.repository.GuiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

/**
 * Orquesta el ciclo de vida de una guia de despacho:
 *   crear (genera PDF en EFS) -> subir a S3 -> descargar / modificar / eliminar / consultar.
 *
 * El flujo de almacenamiento siempre es: primero el EFS (temporal) y luego S3 (definitivo),
 * tal como exige el caso de la semana 3.
 */
@Service
@Transactional
public class GuiaService {

    private final GuiaRepository guiaRepository;
    private final EfsStorageService efsStorage;
    private final S3StorageService s3Storage;
    private final EntityMapper mapper;
    private final GuiaEventProducer eventProducer;

    public GuiaService(GuiaRepository guiaRepository, EfsStorageService efsStorage,
                       S3StorageService s3Storage, EntityMapper mapper,
                       GuiaEventProducer eventProducer) {
        this.guiaRepository = guiaRepository;
        this.efsStorage = efsStorage;
        this.s3Storage = s3Storage;
        this.mapper = mapper;
        this.eventProducer = eventProducer;
    }

    // ---- Endpoint 1: crear guia de despacho (genera el PDF en el EFS) ----
    public GuiaDTO crear(CrearGuiaRequest req) {
        String numero = resolverNumero(req.getNumeroGuia());
        if (guiaRepository.existsByNumeroGuia(numero)) {
            throw new StorageException("Ya existe una guia con numero " + numero);
        }

        Guia guia = mapper.toEntity(req, numero);
        // 1) generar el PDF en el almacenamiento temporal EFS
        Path archivo = efsStorage.generarPdf(guia);
        guia.setEfsPath(archivo.toString());
        GuiaDTO dto = mapper.toDTO(guiaRepository.save(guia));

        // Semana 8: publicar el evento en la Cola 1. Si el request pide forzar un error,
        // el evento se marca para que el consumidor lo rechace (y caiga en la Cola 2).
        EventoGuiaDTO evento = EventoGuiaDTO.de("CREADA", dto);
        evento.setForzarError(req.isForzarError());
        eventProducer.publicar(evento);
        return dto;
    }

    // ---- Endpoint 2: subir la guia generada a S3 ----
    public GuiaDTO subirAS3(Long id) {
        Guia guia = obtener(id);
        Path archivo = rutaEfs(guia);
        // si el PDF temporal no esta en el EFS, se regenera antes de subir
        if (!archivo.toFile().exists()) {
            archivo = efsStorage.generarPdf(guia);
            guia.setEfsPath(archivo.toString());
        }
        String key = s3Storage.subir(guia, archivo);
        guia.setS3Key(key);
        guia.setSubidaS3(true);
        GuiaDTO dto = mapper.toDTO(guiaRepository.save(guia));
        eventProducer.publicar(EventoGuiaDTO.de("SUBIDA_S3", dto)); // Semana 8
        return dto;
    }

    // ---- Endpoint 3: descargar guia con validacion de permisos ----
    // Regla de permiso: solo el transportista dueño de la guia puede descargarla.
    public byte[] descargar(Long id, String transportistaSolicitante) {
        Guia guia = obtener(id);
        validarPermiso(guia, transportistaSolicitante);

        // Preferir S3 (definitivo); si aun no esta subida, servir desde el EFS
        if (guia.isSubidaS3() && guia.getS3Key() != null) {
            return s3Storage.descargar(guia.getS3Key());
        }
        Path archivo = rutaEfs(guia);
        if (!archivo.toFile().exists()) {
            throw new ResourceNotFoundException(
                    "La guia " + guia.getNumeroGuia() + " no tiene archivo disponible (ni en S3 ni en EFS)");
        }
        return efsStorage.leerPdf(archivo);
    }

    // ---- Endpoint 4: modificar / actualizar guia (regenera PDF y re-sube a S3) ----
    public GuiaDTO actualizar(Long id, ActualizarGuiaRequest req) {
        Guia guia = obtener(id);
        boolean estabaEnS3 = guia.isSubidaS3();
        String keyAnterior = guia.getS3Key();

        guia.setTransportista(req.getTransportista());
        guia.setDestinatario(req.getDestinatario());
        guia.setDireccion(req.getDireccion());
        guia.setDescripcion(req.getDescripcion());
        guia.setFechaDespacho(req.getFechaDespacho());

        // regenerar el PDF en el EFS con los datos nuevos
        Path archivo = efsStorage.generarPdf(guia);
        guia.setEfsPath(archivo.toString());

        // si ya estaba en S3, re-subir; si la key cambio (cambio fecha/transportista) borrar la vieja
        if (estabaEnS3) {
            String nuevaKey = s3Storage.subir(guia, archivo);
            if (keyAnterior != null && !keyAnterior.equals(nuevaKey)) {
                s3Storage.eliminar(keyAnterior);
            }
            guia.setS3Key(nuevaKey);
            guia.setSubidaS3(true);
        }
        GuiaDTO dto = mapper.toDTO(guiaRepository.save(guia));
        eventProducer.publicar(EventoGuiaDTO.de("ACTUALIZADA", dto)); // Semana 8
        return dto;
    }

    // ---- Endpoint 5: eliminar guia (borra de S3, EFS y BD) ----
    public void eliminar(Long id) {
        Guia guia = obtener(id);
        if (guia.getS3Key() != null) {
            s3Storage.eliminar(guia.getS3Key());
        }
        efsStorage.eliminarPdf(rutaEfs(guia));
        // Semana 8: capturar los datos para el evento ANTES de borrar la entidad
        GuiaDTO dto = mapper.toDTO(guia);
        guiaRepository.delete(guia);
        eventProducer.publicar(EventoGuiaDTO.de("ELIMINADA", dto));
    }

    // ---- Endpoint 6: consultar guias por transportista y/o fecha (historial) ----
    public List<GuiaDTO> consultar(String transportista, LocalDate fecha) {
        List<Guia> guias;
        if (transportista != null && fecha != null) {
            guias = guiaRepository.findByTransportistaAndFechaDespacho(transportista, fecha);
        } else if (transportista != null) {
            guias = guiaRepository.findByTransportista(transportista);
        } else if (fecha != null) {
            guias = guiaRepository.findByFechaDespacho(fecha);
        } else {
            guias = guiaRepository.findAll();
        }
        return mapper.toGuiaDTOList(guias);
    }

    // Detalle individual (util para el front / validaciones)
    public GuiaDTO obtenerDTO(Long id) {
        return mapper.toDTO(obtener(id));
    }

    // ---- helpers ----
    private Guia obtener(Long id) {
        return guiaRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.guia(id));
    }

    private void validarPermiso(Guia guia, String transportistaSolicitante) {
        if (transportistaSolicitante == null || transportistaSolicitante.isBlank()) {
            throw new StorageException("Debe indicar el transportista solicitante para descargar la guia");
        }
        if (!guia.getTransportista().equalsIgnoreCase(transportistaSolicitante.trim())) {
            throw new ResourceNotFoundException(
                    "El transportista '" + transportistaSolicitante + "' no tiene permiso sobre la guia "
                            + guia.getNumeroGuia());
        }
    }

    private Path rutaEfs(Guia guia) {
        if (guia.getEfsPath() != null) {
            return Paths.get(guia.getEfsPath());
        }
        return efsStorage.getEfsRoot().resolve(efsStorage.nombreArchivo(guia));
    }

    // Si no se envia numero, genera un correlativo basado en el ultimo id + epoch corto
    private String resolverNumero(String numeroPedido) {
        if (numeroPedido != null && !numeroPedido.isBlank()) {
            return numeroPedido.trim();
        }
        long siguiente = guiaRepository.count() + 1;
        return String.valueOf(siguiente);
    }
}
