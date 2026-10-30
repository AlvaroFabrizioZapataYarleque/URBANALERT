package com.utp.urbanalert.dto;

import com.utp.urbanalert.model.EstadoIncidencia;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Datos de entrada para cambiar el estado de una incidencia (PUT). */
@Data
public class CambioEstadoRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoIncidencia estado;
}
