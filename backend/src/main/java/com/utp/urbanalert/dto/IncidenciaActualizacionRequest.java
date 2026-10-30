package com.utp.urbanalert.dto;

import com.utp.urbanalert.model.CategoriaIncidencia;
import com.utp.urbanalert.model.PrioridadIncidencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Datos editables de una incidencia (PUT /api/incidencias/{id}). No incluye estado ni area. */
@Data
public class IncidenciaActualizacionRequest {

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 150, message = "El titulo no puede superar 150 caracteres")
    private String titulo;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
    private String descripcion;

    @NotNull(message = "La categoria es obligatoria")
    private CategoriaIncidencia categoria;

    @NotNull(message = "La prioridad es obligatoria")
    private PrioridadIncidencia prioridad;

    @Size(max = 200, message = "La direccion no puede superar 200 caracteres")
    private String direccionReferencial;
}
