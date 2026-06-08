package com.duoc.guias.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException guia(Long id) {
        return new ResourceNotFoundException("Guia with id " + id + " was not found");
    }

    public static ResourceNotFoundException guiaNumero(String numeroGuia) {
        return new ResourceNotFoundException("Guia with numero " + numeroGuia + " was not found");
    }
}
