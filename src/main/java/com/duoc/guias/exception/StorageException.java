package com.duoc.guias.exception;

/**
 * Error al interactuar con el almacenamiento (EFS o S3): fallo al generar el PDF,
 * al escribir en el disco temporal o al subir/descargar el objeto en S3.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
