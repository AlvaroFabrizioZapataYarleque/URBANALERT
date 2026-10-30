package com.utp.urbanalert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos de salida al consultar un ciudadano. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CiudadanoResponse {

    private Long id;
    private String nombre;
    private String dni;
    private String email;
    private String telefono;
}
