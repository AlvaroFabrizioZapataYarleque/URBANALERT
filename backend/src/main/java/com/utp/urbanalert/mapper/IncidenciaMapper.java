package com.utp.urbanalert.mapper;

import com.utp.urbanalert.dto.HistorialResponse;
import com.utp.urbanalert.dto.IncidenciaResponse;
import com.utp.urbanalert.model.AreaResponsable;
import com.utp.urbanalert.model.HistorialIncidencia;
import com.utp.urbanalert.model.Incidencia;
import org.springframework.stereotype.Component;

/** Convierte entidades de incidencia en DTOs de salida. Compartido por varios servicios. */
@Component
public class IncidenciaMapper {

    public IncidenciaResponse toResponse(Incidencia i) {
        AreaResponsable area = i.getAreaResponsable();
        return IncidenciaResponse.builder()
                .id(i.getId())
                .titulo(i.getTitulo())
                .descripcion(i.getDescripcion())
                .categoria(i.getCategoria())
                .prioridad(i.getPrioridad())
                .estado(i.getEstado())
                .direccionReferencial(i.getDireccionReferencial())
                .fechaReporte(i.getFechaReporte())
                .fechaActualizacion(i.getFechaActualizacion())
                .fechaResolucion(i.getFechaResolucion())
                .ciudadanoId(i.getCiudadano().getId())
                .ciudadanoNombre(i.getCiudadano().getNombre())
                .areaResponsableId(area != null ? area.getId() : null)
                .areaResponsableNombre(area != null ? area.getNombre() : null)
                .build();
    }

    public HistorialResponse toResponse(HistorialIncidencia h) {
        return HistorialResponse.builder()
                .id(h.getId())
                .estadoAnterior(h.getEstadoAnterior())
                .estadoNuevo(h.getEstadoNuevo())
                .detalle(h.getDetalle())
                .realizadoPor(h.getRealizadoPor())
                .fecha(h.getFecha())
                .build();
    }
}
