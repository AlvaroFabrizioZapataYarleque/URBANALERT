import { NombreRol, Permiso } from '../models/api.models';

/**
 * Copia de los permisos que DataLoader asigna a cada rol.
 * El login solo devuelve roles, asi que la UI usa esta tabla para mostrar u ocultar acciones.
 * Es solo presentacion: el backend vuelve a autorizar cada solicitud (@PreAuthorize).
 */
const PERMISOS_POR_ROL: Record<NombreRol, readonly Permiso[]> = {
  ADMIN: [
    'INCIDENCIA_CREAR',
    'INCIDENCIA_VER',
    'INCIDENCIA_EDITAR',
    'INCIDENCIA_CAMBIAR_ESTADO',
    'INCIDENCIA_ASIGNAR_AREA',
    'INCIDENCIA_ELIMINAR',
    'CIUDADANO_GESTIONAR',
    'AREA_GESTIONAR',
    'REPORTE_VER',
  ],
  OPERADOR: ['INCIDENCIA_CREAR', 'INCIDENCIA_VER', 'INCIDENCIA_CAMBIAR_ESTADO', 'CIUDADANO_GESTIONAR'],
  CIUDADANO: ['INCIDENCIA_CREAR', 'INCIDENCIA_VER', 'INCIDENCIA_EDITAR', 'INCIDENCIA_ELIMINAR'],
};

export function permisosDe(roles: readonly NombreRol[]): Set<Permiso> {
  return new Set(roles.flatMap((r) => PERMISOS_POR_ROL[r] ?? []));
}
