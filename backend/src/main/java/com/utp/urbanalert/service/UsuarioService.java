package com.utp.urbanalert.service;

import com.utp.urbanalert.dto.CrearUsuarioRequest;
import com.utp.urbanalert.dto.UsuarioResponse;
import com.utp.urbanalert.security.UsuarioPrincipal;

import java.util.List;

public interface UsuarioService {
    List<UsuarioResponse> listarTodos();

    /** Alta de personal municipal (ADMIN u OPERADOR). */
    UsuarioResponse crear(CrearUsuarioRequest request);

    UsuarioResponse cambiarHabilitado(Long id, boolean habilitado, UsuarioPrincipal actor);
}
