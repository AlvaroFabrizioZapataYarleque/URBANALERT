package com.utp.urbanalert.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Alta de un ciudadano por personal municipal (atencion presencial o telefonica),
 * sin cuenta de acceso. Los ciudadanos que se registran solos usan /api/auth/register.
 */
@Data
public class CiudadanoRequest {

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
