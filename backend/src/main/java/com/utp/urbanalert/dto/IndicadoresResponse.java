package com.utp.urbanalert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Indicadores para el dashboard municipal. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicadoresResponse {

    private long total;
    private Map<String, Long> porEstado;
    private Map<String, Long> porCategoria;
    /** Umbral (en dias) usado para contar las pendientes antiguas. */
    private int diasUmbral;
    /** Incidencias sin resolver reportadas hace mas de diasUmbral dias. */
    private long pendientesAntiguas;
    /** Promedio de horas entre el reporte y la resolucion (0 si aun no hay resueltas). */
    private double tiempoPromedioResolucionHoras;
    private Map<String, Double> tiempoPromedioResolucionHorasPorCategoria;
}
