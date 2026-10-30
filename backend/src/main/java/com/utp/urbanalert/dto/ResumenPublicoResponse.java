package com.utp.urbanalert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Cifras generales abiertas a cualquier visitante (sin datos personales). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenPublicoResponse {

    private long total;
    private Map<String, Long> porEstado;
    private Map<String, Long> porCategoria;
}
