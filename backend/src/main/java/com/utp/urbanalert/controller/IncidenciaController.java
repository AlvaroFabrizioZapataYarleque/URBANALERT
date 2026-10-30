package com.utp.urbanalert.controller;

import com.utp.urbanalert.dto.CambioAreaRequest;
import com.utp.urbanalert.dto.CambioEstadoRequest;
import com.utp.urbanalert.dto.HistorialResponse;
import com.utp.urbanalert.dto.IncidenciaActualizacionRequest;
import com.utp.urbanalert.dto.IncidenciaFiltro;
import com.utp.urbanalert.dto.IncidenciaRequest;
import com.utp.urbanalert.dto.IncidenciaResponse;
import com.utp.urbanalert.security.UsuarioPrincipal;
import com.utp.urbanalert.service.IncidenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints REST de Incidencias.
 *
 * Seguridad en dos niveles:
 *  1) @PreAuthorize: el usuario debe tener el PERMISO para la operacion.
 *  2) Servicio: ademas, la incidencia concreta debe estar dentro de su alcance
 *     (un ciudadano solo ve las suyas; un operador, las de su area).
 *
 * IoC / DI: el controlador recibe la interfaz del servicio por constructor
 * (generado por Lombok); Spring inyecta la implementacion concreta.
 */
@RestController
@RequestMapping("/api/incidencias")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    /**
     * GET /api/incidencias - lista las incidencias del alcance del usuario.
     * Filtros opcionales: estado, categoria, prioridad, areaId, ciudadanoId, desde, hasta (yyyy-MM-dd).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('INCIDENCIA_VER')")
    public ResponseEntity<List<IncidenciaResponse>> listar(IncidenciaFiltro filtro,
                                                           @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(incidenciaService.listar(filtro, actor));
    }

    /** GET /api/incidencias/{id} - detalle de una incidencia. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INCIDENCIA_VER')")
    public ResponseEntity<IncidenciaResponse> obtener(@PathVariable Long id,
                                                      @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(incidenciaService.obtenerPorId(id, actor));
    }

    /** GET /api/incidencias/{id}/historial - seguimiento historico de la incidencia. */
    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAuthority('INCIDENCIA_VER')")
    public ResponseEntity<List<HistorialResponse>> historial(@PathVariable Long id,
                                                             @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(incidenciaService.obtenerHistorial(id, actor));
    }

    /** POST /api/incidencias - registra una nueva incidencia. */
    @PostMapping
    @PreAuthorize("hasAuthority('INCIDENCIA_CREAR')")
    public ResponseEntity<IncidenciaResponse> registrar(@Valid @RequestBody IncidenciaRequest request,
                                                        @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.registrar(request, actor));
    }

    /** PUT /api/incidencias/{id} - edita los datos del reporte (titulo, descripcion, categoria...). */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INCIDENCIA_EDITAR')")
    public ResponseEntity<IncidenciaResponse> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody IncidenciaActualizacionRequest request,
                                                         @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(incidenciaService.actualizar(id, request, actor));
    }

    /** PUT /api/incidencias/{id}/estado - cambia el estado respetando el flujo permitido. */
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('INCIDENCIA_CAMBIAR_ESTADO')")
    public ResponseEntity<IncidenciaResponse> cambiarEstado(@PathVariable Long id,
                                                            @Valid @RequestBody CambioEstadoRequest request,
                                                            @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(incidenciaService.cambiarEstado(id, request.getEstado(), actor));
    }

    /** PUT /api/incidencias/{id}/area - deriva la incidencia a un area responsable. */
    @PutMapping("/{id}/area")
    @PreAuthorize("hasAuthority('INCIDENCIA_ASIGNAR_AREA')")
    public ResponseEntity<IncidenciaResponse> asignarArea(@PathVariable Long id,
                                                          @Valid @RequestBody CambioAreaRequest request,
                                                          @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(incidenciaService.asignarArea(id, request.getAreaId(), actor));
    }

    /** DELETE /api/incidencias/{id} - elimina una incidencia (y su historial). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INCIDENCIA_ELIMINAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @AuthenticationPrincipal UsuarioPrincipal actor) {
        incidenciaService.eliminar(id, actor);
        return ResponseEntity.noContent().build();
    }
}
