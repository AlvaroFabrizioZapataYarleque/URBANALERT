package com.utp.urbanalert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Datos publicos de una cuenta de usuario (nunca incluye la contrasena). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Long id;
    private String username;
    private boolean habilitado;
    private List<String> roles;
    private Long ciudadanoId;
    private Long areaId;
    private String areaNombre;
    private LocalDateTime fechaCreacion;
}
