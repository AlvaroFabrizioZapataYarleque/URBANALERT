package com.utp.urbanalert.controller;

import com.utp.urbanalert.dto.IncidenciaResponse;
import com.utp.urbanalert.dto.IndicadoresResponse;
import com.utp.urbanalert.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dashboard municipal: solo para quien tenga el permiso REPORTE_VER (ADMIN). */
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REPORTE_VER')")
public class ReporteController {

    private final ReporteService reporteService;

    /** GET /api/reportes/indicadores?dias=7 - totales, pendientes antiguas y tiempos de resolucion. */
    @GetMapping("/indicadores")
    public ResponseEntity<IndicadoresResponse> indicadores(@RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(reporteService.indicadores(dias));
    }

    /** GET /api/reportes/pendientes?dias=7 - incidencias sin resolver con mas de N dias. */
    @GetMapping("/pendientes")
    public ResponseEntity<List<IncidenciaResponse>> pendientes(@RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(reporteService.pendientesAntiguas(dias));
    }
}
