package com.utp.urbanalert.service.impl;

import com.utp.urbanalert.dto.IncidenciaResponse;
import com.utp.urbanalert.dto.IndicadoresResponse;
import com.utp.urbanalert.dto.ResumenPublicoResponse;
import com.utp.urbanalert.exception.BusinessValidationException;
import com.utp.urbanalert.mapper.IncidenciaMapper;
import com.utp.urbanalert.model.CategoriaIncidencia;
import com.utp.urbanalert.model.EstadoIncidencia;
import com.utp.urbanalert.repository.IncidenciaRepository;
import com.utp.urbanalert.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Indicadores construidos a partir de las consultas JPQL del repositorio. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

    /** Estados "sin resolver", tomados de la propia definicion del enum (unica fuente de verdad). */
    private static final Set<EstadoIncidencia> ESTADOS_PENDIENTES = EnumSet.allOf(EstadoIncidencia.class).stream()
            .filter(EstadoIncidencia::estaPendiente)
            .collect(Collectors.toSet());

    private final IncidenciaRepository incidenciaRepository;
    private final IncidenciaMapper mapper;

    @Override
    public ResumenPublicoResponse resumenPublico() {
        Map<String, Long> porEstado = agrupar(EstadoIncidencia.class, incidenciaRepository.contarPorEstado());
        Map<String, Long> porCategoria = agrupar(CategoriaIncidencia.class, incidenciaRepository.contarPorCategoria());
        long total = porEstado.values().stream().mapToLong(Long::longValue).sum();
        return ResumenPublicoResponse.builder()
                .total(total)
                .porEstado(porEstado)
                .porCategoria(porCategoria)
                .build();
    }

    @Override
    public IndicadoresResponse indicadores(int diasUmbral) {
        validarDias(diasUmbral);
        ResumenPublicoResponse resumen = resumenPublico();

        long pendientesAntiguas = incidenciaRepository.contarPendientesAnterioresA(
                ESTADOS_PENDIENTES, LocalDateTime.now().minusDays(diasUmbral));

        // Cada fila: [categoria, fechaReporte, fechaResolucion]
        List<Object[]> filas = incidenciaRepository.findTiemposResolucion();
        double promedioGlobal = filas.stream()
                .mapToLong(this::minutosDeResolucion)
                .average()
                .orElse(0) / 60.0;
        Map<String, Double> promedioPorCategoria = filas.stream().collect(Collectors.groupingBy(
                fila -> ((CategoriaIncidencia) fila[0]).name(),
                LinkedHashMap::new,
                Collectors.averagingLong(this::minutosDeResolucion)));
        promedioPorCategoria.replaceAll((categoria, minutos) -> redondear(minutos / 60.0));

        return IndicadoresResponse.builder()
                .total(resumen.getTotal())
                .porEstado(resumen.getPorEstado())
                .porCategoria(resumen.getPorCategoria())
                .diasUmbral(diasUmbral)
                .pendientesAntiguas(pendientesAntiguas)
                .tiempoPromedioResolucionHoras(redondear(promedioGlobal))
                .tiempoPromedioResolucionHorasPorCategoria(promedioPorCategoria)
                .build();
    }

    @Override
    public List<IncidenciaResponse> pendientesAntiguas(int dias) {
        validarDias(dias);
        return incidenciaRepository
                .findPendientesAnterioresA(ESTADOS_PENDIENTES, LocalDateTime.now().minusDays(dias))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    // ---- helpers privados ----

    private void validarDias(int dias) {
        if (dias < 1) {
            throw new BusinessValidationException("El parametro 'dias' debe ser mayor o igual a 1");
        }
    }

    /** Incluye todos los valores del enum (con 0 si no hay filas) para que el resultado sea estable. */
    private <E extends Enum<E>> Map<String, Long> agrupar(Class<E> tipo, List<Object[]> filas) {
        Map<String, Long> resultado = new LinkedHashMap<>();
        for (E valor : tipo.getEnumConstants()) {
            resultado.put(valor.name(), 0L);
        }
        for (Object[] fila : filas) {
            resultado.put(((Enum<?>) fila[0]).name(), ((Number) fila[1]).longValue());
        }
        return resultado;
    }

    private long minutosDeResolucion(Object[] fila) {
        return Duration.between((LocalDateTime) fila[1], (LocalDateTime) fila[2]).toMinutes();
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
