package com.utp.urbanalert.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respuestas JSON para los errores de seguridad que ocurren dentro de la
 * cadena de filtros (antes de llegar a los controladores):
 *  - 401 Unauthorized: no hay token, o es invalido / esta vencido.
 *  - 403 Forbidden: hay sesion pero no se tiene permiso.
 * Sin esto, Spring Security devolveria un 403 vacio para ambos casos.
 */
@Component
@RequiredArgsConstructor
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED,
                "No autenticado: falta el token o es invalido/expirado");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operacion");
    }

    private void escribir(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now());
        cuerpo.put("status", estado.value());
        cuerpo.put("mensaje", mensaje);

        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), cuerpo);
    }
}
