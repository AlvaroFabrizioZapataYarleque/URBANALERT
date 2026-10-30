package com.utp.urbanalert.model;

/**
 * Ciclo de vida de una incidencia. Flujo permitido:
 *
 *   REGISTRADA -> EN_PROCESO -> RESUELTA -> CERRADA
 *                     ^____________|  (una resuelta puede reabrirse)
 *
 * CERRADA es un estado final: no admite mas cambios.
 */
public enum EstadoIncidencia {
    REGISTRADA,
    EN_PROCESO,
    RESUELTA,
    CERRADA;

    /** Indica si es valido pasar de este estado al estado destino. */
    public boolean puedePasarA(EstadoIncidencia destino) {
        return switch (this) {
            case REGISTRADA -> destino == EN_PROCESO;
            case EN_PROCESO -> destino == RESUELTA;
            case RESUELTA -> destino == CERRADA || destino == EN_PROCESO;
            case CERRADA -> false;
        };
    }

    /** Una incidencia esta pendiente mientras no haya sido resuelta. */
    public boolean estaPendiente() {
        return this == REGISTRADA || this == EN_PROCESO;
    }
}
