package com.utp.urbanalert.service;

import com.utp.urbanalert.dto.AuthResponse;
import com.utp.urbanalert.dto.LoginRequest;
import com.utp.urbanalert.dto.RegistroRequest;
import com.utp.urbanalert.dto.UsuarioResponse;

public interface AuthService {

    /** Registra a un ciudadano: crea su perfil y su cuenta (rol CIUDADANO) en una sola transaccion. */
    UsuarioResponse registrar(RegistroRequest request);

    /** Valida las credenciales y devuelve un token JWT. */
    AuthResponse login(LoginRequest request);
}
