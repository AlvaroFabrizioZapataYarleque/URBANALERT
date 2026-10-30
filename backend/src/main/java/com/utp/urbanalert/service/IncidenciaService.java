package com.utp.urbanalert.service;

import com.utp.urbanalert.dto.HistorialResponse;
import com.utp.urbanalert.dto.IncidenciaActualizacionRequest;
import com.utp.urbanalert.dto.IncidenciaFiltro;
import com.utp.urbanalert.dto.IncidenciaRequest;
import com.utp.urbanalert.dto.IncidenciaResponse;
import com.utp.urbanalert.model.EstadoIncidencia;
import com.utp.urbanalert.security.UsuarioPrincipal;

import java.util.List;

/**
 * Contrato del servicio de Incidencias. Los controladores dependen de esta
 * interfaz (no de la implementacion concreta) para aplicar Inversion de
 * Control: Spring decide en tiempo de ejecucion que implementacion inyectar.
 *
 * Todas las operaciones reciben al usuario que las ejecuta ("actor") porque
 * lo que cada rol puede ver o modificar depende de quien es:
 *  - CIUDADANO: solo sus propias incidencias.
 *  - OPERADOR: solo las incidencias de su area.
 *  - ADMIN: todas.
 */
public interface IncidenciaService {

    List<IncidenciaResponse> listar(IncidenciaFiltro filtro, UsuarioPrincipal actor);

    IncidenciaResponse obtenerPorId(Long id, UsuarioPrincipal actor);

    List<HistorialResponse> obtenerHistorial(Long id, UsuarioPrincipal actor);

    IncidenciaResponse registrar(IncidenciaRequest request, UsuarioPrincipal actor);

    IncidenciaResponse actualizar(Long id, IncidenciaActualizacionRequest request, UsuarioPrincipal actor);

    IncidenciaResponse cambiarEstado(Long id, EstadoIncidencia nuevoEstado, UsuarioPrincipal actor);

    IncidenciaResponse asignarArea(Long id, Long areaId, UsuarioPrincipal actor);

    void eliminar(Long id, UsuarioPrincipal actor);
}
