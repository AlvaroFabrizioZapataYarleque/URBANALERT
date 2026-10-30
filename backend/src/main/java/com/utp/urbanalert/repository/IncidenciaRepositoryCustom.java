package com.utp.urbanalert.repository;

import com.utp.urbanalert.dto.IncidenciaFiltro;
import com.utp.urbanalert.model.Incidencia;

import java.util.List;

/**
 * Consultas que Spring Data no puede derivar del nombre del metodo porque
 * sus condiciones son opcionales (fragmento de repositorio personalizado).
 */
public interface IncidenciaRepositoryCustom {

    /** Busca incidencias aplicando solo los criterios que vengan informados en el filtro. */
    List<Incidencia> buscar(IncidenciaFiltro filtro);
}
