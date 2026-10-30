package com.utp.urbanalert.service;

import com.utp.urbanalert.dto.IncidenciaResponse;
import com.utp.urbanalert.dto.IndicadoresResponse;
import com.utp.urbanalert.dto.ResumenPublicoResponse;

import java.util.List;

public interface ReporteService {

    /** Cifras generales sin datos personales (accesibles sin autenticacion). */
    ResumenPublicoResponse resumenPublico();

    /** Indicadores completos para el dashboard municipal. */
    IndicadoresResponse indicadores(int diasUmbral);

    /** Incidencias sin resolver reportadas hace mas de "dias" dias. */
    List<IncidenciaResponse> pendientesAntiguas(int dias);
}
