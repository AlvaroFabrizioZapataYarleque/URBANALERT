package com.utp.urbanalert.exception;

/** Se lanza cuando la operacion choca con datos existentes (usuario, DNI o nombre duplicado). HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
