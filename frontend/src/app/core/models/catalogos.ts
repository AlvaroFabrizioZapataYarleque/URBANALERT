import { CategoriaIncidencia, EstadoIncidencia, PrioridadIncidencia } from './api.models';

/** Etiquetas de presentacion para los enums del backend. El orden sigue al de los enums Java. */

export const ESTADOS: readonly { value: EstadoIncidencia; label: string }[] = [
  { value: 'REGISTRADA', label: 'Registrada' },
  { value: 'EN_PROCESO', label: 'En proceso' },
  { value: 'RESUELTA', label: 'Resuelta' },
  { value: 'CERRADA', label: 'Cerrada' },
];

export const PRIORIDADES: readonly { value: PrioridadIncidencia; label: string }[] = [
  { value: 'BAJA', label: 'Baja' },
  { value: 'MEDIA', label: 'Media' },
  { value: 'ALTA', label: 'Alta' },
  { value: 'CRITICA', label: 'Crítica' },
];

export const CATEGORIAS: readonly { value: CategoriaIncidencia; label: string }[] = [
  { value: 'ALUMBRADO_PUBLICO', label: 'Alumbrado público' },
  { value: 'PISTAS', label: 'Pistas' },
  { value: 'VEREDAS', label: 'Veredas' },
  { value: 'SEMAFOROS', label: 'Semáforos' },
  { value: 'RESIDUOS', label: 'Residuos sólidos' },
  { value: 'SENALIZACION', label: 'Señalización' },
  { value: 'INFRAESTRUCTURA_PUBLICA', label: 'Infraestructura pública' },
];

export type CatalogoTipo = 'estado' | 'prioridad' | 'categoria' | 'rol';

const ROLES: Record<string, string> = {
  ADMIN: 'Administrador municipal',
  OPERADOR: 'Operador de área',
  CIUDADANO: 'Ciudadano',
};

export function etiqueta(tipo: CatalogoTipo, valor: string | null | undefined): string {
  if (!valor) return '';
  const lista = tipo === 'estado' ? ESTADOS : tipo === 'prioridad' ? PRIORIDADES : tipo === 'categoria' ? CATEGORIAS : null;
  if (!lista) return ROLES[valor] ?? valor;
  return lista.find((x) => x.value === valor)?.label ?? valor;
}

/**
 * Transiciones que acepta EstadoIncidencia.puedePasarA() en el backend.
 * Se usan solo para ofrecer opciones validas en la UI; el servidor vuelve a validarlas.
 */
export const TRANSICIONES: Record<EstadoIncidencia, EstadoIncidencia[]> = {
  REGISTRADA: ['EN_PROCESO'],
  EN_PROCESO: ['RESUELTA'],
  RESUELTA: ['CERRADA', 'EN_PROCESO'],
  CERRADA: [],
};

/** Mismo criterio que EstadoIncidencia.estaPendiente(). */
export function estaPendiente(estado: EstadoIncidencia): boolean {
  return estado === 'REGISTRADA' || estado === 'EN_PROCESO';
}
