package com.utp.urbanalert.controller;

import com.utp.urbanalert.dto.AuthResponse;
import com.utp.urbanalert.dto.LoginRequest;
import com.utp.urbanalert.dto.RegistroRequest;
import com.utp.urbanalert.dto.UsuarioResponse;
import com.utp.urbanalert.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints publicos: no requieren token (ver SecurityConfig). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/register - un ciudadano crea su cuenta. */
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    /** POST /api/auth/login - valida credenciales y devuelve el token JWT. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
