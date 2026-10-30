package com.utp.urbanalert.exception;

/**
 * Se lanza cuando una regla de negocio no se cumple (distinto de los
 * errores de formato que ya cubren las anotaciones @NotBlank/@NotNull
 * de los DTO). Ejemplo: transiciones de estado invalidas.
 */
public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) {
        super(message);
    }
}
