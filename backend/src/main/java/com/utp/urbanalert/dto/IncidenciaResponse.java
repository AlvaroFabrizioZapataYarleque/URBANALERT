package com.utp.urbanalert.dto;

import com.utp.urbanalert.model.CategoriaIncidencia;
import com.utp.urbanalert.model.EstadoIncidencia;
import com.utp.urbanalert.model.PrioridadIncidencia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Datos de salida al consultar una incidencia. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidenciaResponse {

    private Long id;
    private String titulo;
    private String descripcion;
    private CategoriaIncidencia categoria;
    private PrioridadIncidencia prioridad;
    private EstadoIncidencia estado;
    private String direccionReferencial;
    private LocalDateTime fechaReporte;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime fechaResolucion;
    private Long ciudadanoId;
    private String ciudadanoNombre;
    private Long areaResponsableId;
    private String areaResponsableNombre;
}
