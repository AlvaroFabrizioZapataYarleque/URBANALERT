package com.utp.urbanalert.dto;

import com.utp.urbanalert.model.EstadoIncidencia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Una entrada del seguimiento historico de una incidencia. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialResponse {

    private Long id;
    private EstadoIncidencia estadoAnterior;
    private EstadoIncidencia estadoNuevo;
    private String detalle;
    private String realizadoPor;
    private LocalDateTime fecha;
}
