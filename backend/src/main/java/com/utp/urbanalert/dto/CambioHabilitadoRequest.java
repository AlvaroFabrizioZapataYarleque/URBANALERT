package com.utp.urbanalert.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Habilita o deshabilita una cuenta (PUT /api/usuarios/{id}/habilitado). */
@Data
public class CambioHabilitadoRequest {

    @NotNull(message = "El campo habilitado es obligatorio")
    private Boolean habilitado;
}
