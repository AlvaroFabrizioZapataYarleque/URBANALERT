package com.utp.urbanalert.model;

/**
 * Permisos granulares asociados a los roles. Spring Security los expone como
 * autoridades (ej. hasAuthority('INCIDENCIA_CREAR')), de modo que los
 * endpoints se protegen por lo que se puede HACER y no solo por quien es el
 * usuario.
 */
public enum Permiso {
    INCIDENCIA_CREAR,
    INCIDENCIA_VER,
    INCIDENCIA_EDITAR,
    INCIDENCIA_CAMBIAR_ESTADO,
    INCIDENCIA_ASIGNAR_AREA,
    INCIDENCIA_ELIMINAR,
    CIUDADANO_GESTIONAR,
    AREA_GESTIONAR,
    REPORTE_VER
}
