package com.utp.urbanalert.controller;

import com.utp.urbanalert.dto.CiudadanoRequest;
import com.utp.urbanalert.dto.CiudadanoResponse;
import com.utp.urbanalert.dto.PerfilRequest;
import com.utp.urbanalert.security.UsuarioPrincipal;
import com.utp.urbanalert.service.CiudadanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ciudadanos")
@RequiredArgsConstructor
public class CiudadanoController {

    private final CiudadanoService ciudadanoService;

    /** GET /api/ciudadanos - lista ciudadanos (personal municipal). */
    @GetMapping
    @PreAuthorize("hasAuthority('CIUDADANO_GESTIONAR')")
    public ResponseEntity<List<CiudadanoResponse>> listar() {
        return ResponseEntity.ok(ciudadanoService.listarTodos());
    }

    /** GET /api/ciudadanos/me - perfil del ciudadano autenticado. */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CIUDADANO')")
    public ResponseEntity<CiudadanoResponse> miPerfil(@AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(ciudadanoService.obtenerMiPerfil(actor));
    }

    /** PUT /api/ciudadanos/me - el ciudadano actualiza sus datos de contacto. */
    @PutMapping("/me")
    @PreAuthorize("hasRole('CIUDADANO')")
    public ResponseEntity<CiudadanoResponse> actualizarMiPerfil(@Valid @RequestBody PerfilRequest request,
                                                                @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(ciudadanoService.actualizarMiPerfil(request, actor));
    }

    /** GET /api/ciudadanos/{id} - obtiene un ciudadano por id (personal municipal). */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CIUDADANO_GESTIONAR')")
    public ResponseEntity<CiudadanoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ciudadanoService.obtenerPorId(id));
    }

    /** POST /api/ciudadanos - registra un ciudadano sin cuenta (atencion presencial/telefonica). */
    @PostMapping
    @PreAuthorize("hasAuthority('CIUDADANO_GESTIONAR')")
    public ResponseEntity<CiudadanoResponse> registrar(@Valid @RequestBody CiudadanoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ciudadanoService.registrar(request));
    }
}
