package com.utp.urbanalert.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Datos de entrada para asignar o reasignar el area responsable de una incidencia (PUT). */
@Data
public class CambioAreaRequest {

    @NotNull(message = "El id del area responsable es obligatorio")
    private Long areaId;
}
