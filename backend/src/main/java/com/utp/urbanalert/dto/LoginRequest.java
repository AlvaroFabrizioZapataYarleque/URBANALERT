package com.utp.urbanalert.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Credenciales para iniciar sesion (POST /api/auth/login). */
@Data
public class LoginRequest {

    @NotBlank(message = "El usuario es obligatorio")
    private String username;

    @NotBlank(message = "La contrasena es obligatoria")
    private String password;
}
