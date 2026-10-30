package com.utp.urbanalert.controller;

import com.utp.urbanalert.dto.AreaRequest;
import com.utp.urbanalert.dto.AreaResponse;
import com.utp.urbanalert.service.AreaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    /** GET /api/areas - cualquier usuario autenticado puede consultar las areas. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AreaResponse>> listar() {
        return ResponseEntity.ok(areaService.listarTodas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AreaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(areaService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('AREA_GESTIONAR')")
    public ResponseEntity<AreaResponse> crear(@Valid @RequestBody AreaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(areaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('AREA_GESTIONAR')")
    public ResponseEntity<AreaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody AreaRequest request) {
        return ResponseEntity.ok(areaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('AREA_GESTIONAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        areaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
