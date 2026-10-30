package com.utp.urbanalert.service;

import com.utp.urbanalert.dto.CiudadanoRequest;
import com.utp.urbanalert.dto.CiudadanoResponse;
import com.utp.urbanalert.dto.PerfilRequest;
import com.utp.urbanalert.security.UsuarioPrincipal;

import java.util.List;

public interface CiudadanoService {
    List<CiudadanoResponse> listarTodos();

    CiudadanoResponse obtenerPorId(Long id);

    /** Alta de un ciudadano sin cuenta de acceso (atencion presencial o telefonica). */
    CiudadanoResponse registrar(CiudadanoRequest request);

    CiudadanoResponse obtenerMiPerfil(UsuarioPrincipal actor);

    CiudadanoResponse actualizarMiPerfil(PerfilRequest request, UsuarioPrincipal actor);
}
