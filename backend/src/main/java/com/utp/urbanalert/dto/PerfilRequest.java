package com.utp.urbanalert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Datos que un ciudadano puede modificar de su propio perfil (el DNI no se puede cambiar). */
@Data
public class PerfilRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    private String nombre;

    @Email(message = "El email no tiene un formato valido")
    @Size(max = 120, message = "El email no puede superar 120 caracteres")
    private String email;

    @Pattern(regexp = "\\d{9}", message = "El telefono debe tener 9 digitos")
    private String telefono;
}
