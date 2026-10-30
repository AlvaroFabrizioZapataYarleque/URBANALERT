package com.utp.urbanalert.model;

/**
 * Roles del sistema. Spring Security los expone como autoridades con el
 * prefijo "ROLE_" (ej. ROLE_ADMIN).
 *
 * - ADMIN: administrador municipal.
 * - OPERADOR: personal de un area municipal que atiende incidencias.
 * - CIUDADANO: vecino que reporta incidencias.
 * Un visitante sin sesion (GUEST) no tiene rol: solo accede a los
 * endpoints publicos.
 */
public enum NombreRol {
    ADMIN,
    OPERADOR,
    CIUDADANO
}
