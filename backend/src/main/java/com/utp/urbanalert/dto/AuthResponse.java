package com.utp.urbanalert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Respuesta del login: el token que el cliente debe enviar en cada solicitud. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    /** Esquema de la cabecera Authorization: "Authorization: Bearer <token>". */
    private String tipo;
    private long expiraEnSegundos;
    private String username;
    private List<String> roles;
}
