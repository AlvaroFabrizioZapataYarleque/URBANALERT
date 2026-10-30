package com.utp.urbanalert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Datos para que un ciudadano cree su cuenta (POST /api/auth/register). */
@Data
public class RegistroRequest {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 4, max = 50, message = "El usuario debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
            message = "El usuario solo admite letras, numeros, punto, guion y guion bajo")
    private String username;

    // BCrypt solo procesa los primeros 72 bytes, por eso ese es el maximo.
    @NotBlank(message = "La contrasena es obligatoria")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
            message = "La contrasena debe tener entre 8 y 72 caracteres, con al menos una letra y un numero")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    private String nombre;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 digitos")
    private String dni;

    @Email(message = "El email no tiene un formato valido")
    @Size(max = 120, message = "El email no puede superar 120 caracteres")
    private String email;

    @Pattern(regexp = "\\d{9}", message = "El telefono debe tener 9 digitos")
    private String telefono;
}
