package com.utp.urbanalert.controller;

import com.utp.urbanalert.dto.CambioHabilitadoRequest;
import com.utp.urbanalert.dto.CrearUsuarioRequest;
import com.utp.urbanalert.dto.UsuarioResponse;
import com.utp.urbanalert.security.UsuarioPrincipal;
import com.utp.urbanalert.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administracion de cuentas. Toda la clase es solo para ADMIN: se usa
 * @Secured (regla simple por rol) en lugar de @PreAuthorize (expresiones).
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Secured("ROLE_ADMIN")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    /** POST /api/usuarios - alta de personal municipal (ADMIN u OPERADOR). */
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    /** PUT /api/usuarios/{id}/habilitado - habilita o deshabilita una cuenta. */
    @PutMapping("/{id}/habilitado")
    public ResponseEntity<UsuarioResponse> cambiarHabilitado(@PathVariable Long id,
                                                             @Valid @RequestBody CambioHabilitadoRequest request,
                                                             @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(usuarioService.cambiarHabilitado(id, request.getHabilitado(), actor));
    }
}
