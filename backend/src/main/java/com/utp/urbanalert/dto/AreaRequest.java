package com.utp.urbanalert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Datos para crear o editar un area responsable. */
@Data
public class AreaRequest {

    @NotBlank(message = "El nombre del area es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @Size(max = 250, message = "La descripcion no puede superar 250 caracteres")
    private String descripcion;
}
