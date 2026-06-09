package com.duoc.guias.exception;

/** Credenciales invalidas en el login (se mapea a 401 Unauthorized). */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
