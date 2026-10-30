package com.utp.urbanalert.dto;

import com.utp.urbanalert.model.CategoriaIncidencia;
import com.utp.urbanalert.model.PrioridadIncidencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Datos de entrada para registrar una nueva incidencia.
 *
 * ciudadanoId y areaId son SOLO para personal municipal (ADMIN/OPERADOR) que
 * registra a nombre de un vecino. Cuando quien registra es un CIUDADANO, el
 * servidor ignora ambos campos: la incidencia siempre queda a nombre de su
 * propio perfil y sin area (nunca se confia en la propiedad que envia el cliente).
 */
@Data
public class IncidenciaRequest {

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

    /** Solo personal municipal: ciudadano a cuyo nombre se registra. */
    private Long ciudadanoId;

    /** Solo personal municipal: area a la que se deriva desde el registro (opcional). */
    private Long areaId;
}
