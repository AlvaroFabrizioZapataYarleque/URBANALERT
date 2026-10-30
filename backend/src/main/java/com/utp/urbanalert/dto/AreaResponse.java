package com.utp.urbanalert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos de salida al consultar un area responsable. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
}
