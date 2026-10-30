package com.utp.urbanalert.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centraliza el manejo de errores para que todos los controladores devuelvan
 * respuestas JSON consistentes en lugar de stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessValidation(BusinessValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex) {
        // Para violaciones de integridad de la BD no se expone el detalle interno.
        String mensaje = ex instanceof ConflictException
                ? ex.getMessage()
                : "La operacion viola una restriccion de datos (valor duplicado o en uso)";
        return buildResponse(HttpStatus.CONFLICT, mensaje);
    }

    // 401: credenciales incorrectas o usuario deshabilitado al iniciar sesion.
    // Mensaje generico a proposito: no revela si el usuario existe.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Usuario o contrasena incorrectos");
    }

    // 403: @PreAuthorize/@Secured denegado, o regla de acceso a nivel de dato (ej. incidencia ajena).
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // Errores de @Valid en el cuerpo (@NotBlank, @Pattern, ...) y de conversion de query params del filtro.
    // MethodArgumentNotValidException es subclase de BindException, por lo que este metodo cubre ambos.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(BindException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                errores.put(err.getField(), err.getDefaultMessage()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("errores", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // JSON mal formado o con un valor de enum inexistente en el cuerpo.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es invalido o esta mal formado");
    }

    // Parametro de ruta o query con tipo/valor invalido (ej. estado=XYZ, desde=hoy).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Valor invalido para el parametro '" + ex.getName() + "'");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("mensaje", message);
        return ResponseEntity.status(status).body(body);
    }
}
