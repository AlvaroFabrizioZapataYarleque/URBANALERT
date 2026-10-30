package com.utp.urbanalert.controller;

import com.utp.urbanalert.dto.ResumenPublicoResponse;
import com.utp.urbanalert.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Informacion abierta a visitantes (GUEST): no requiere token y no contiene datos personales. */
@RestController
@RequestMapping("/api/publico")
@RequiredArgsConstructor
public class PublicoController {

    private final ReporteService reporteService;

    /** GET /api/publico/resumen - cantidad de incidencias por estado y categoria. */
    @GetMapping("/resumen")
    public ResponseEntity<ResumenPublicoResponse> resumen() {
        return ResponseEntity.ok(reporteService.resumenPublico());
    }
}
