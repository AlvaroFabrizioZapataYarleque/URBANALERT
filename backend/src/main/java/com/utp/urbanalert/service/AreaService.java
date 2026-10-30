package com.utp.urbanalert.service;

import com.utp.urbanalert.dto.AreaRequest;
import com.utp.urbanalert.dto.AreaResponse;

import java.util.List;

public interface AreaService {
    List<AreaResponse> listarTodas();

    AreaResponse obtenerPorId(Long id);

    AreaResponse crear(AreaRequest request);

    AreaResponse actualizar(Long id, AreaRequest request);

    void eliminar(Long id);
}
