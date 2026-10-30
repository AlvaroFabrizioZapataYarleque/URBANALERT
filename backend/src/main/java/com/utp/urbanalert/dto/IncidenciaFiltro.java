package com.utp.urbanalert.dto;

import com.utp.urbanalert.model.CategoriaIncidencia;
import com.utp.urbanalert.model.EstadoIncidencia;
import com.utp.urbanalert.model.PrioridadIncidencia;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Criterios opcionales para filtrar incidencias (query params de
 * GET /api/incidencias). Los campos nulos simplemente no filtran.
 * Las fechas se envian como yyyy-MM-dd y ambas son inclusivas.
 */
@Data
public class IncidenciaFiltro {

    private EstadoIncidencia estado;
    private CategoriaIncidencia categoria;
    private PrioridadIncidencia prioridad;
    private Long areaId;
    private Long ciudadanoId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate desde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hasta;
}
