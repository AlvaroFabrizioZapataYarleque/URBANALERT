package com.utp.urbanalert.dto;

import com.utp.urbanalert.model.NombreRol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Alta de personal municipal por un administrador (POST /api/usuarios).
 * Solo admite los roles ADMIN y OPERADOR; los ciudadanos se registran solos.
 */
@Data
public class CrearUsuarioRequest {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 4, max = 50, message = "El usuario debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
            message = "El usuario solo admite letras, numeros, punto, guion y guion bajo")
    private String username;

    @NotBlank(message = "La contrasena es obligatoria")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
            message = "La contrasena debe tener entre 8 y 72 caracteres, con al menos una letra y un numero")
    private String password;

    @NotNull(message = "El rol es obligatorio (ADMIN u OPERADOR)")
    private NombreRol rol;

    /** Obligatorio para OPERADOR (area en la que trabaja); se ignora para ADMIN. */
    private Long areaId;
}
