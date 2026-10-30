package com.utp.urbanalert.exception;

/** Se lanza cuando se busca una entidad (Ciudadano, Incidencia, Area) que no existe. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
