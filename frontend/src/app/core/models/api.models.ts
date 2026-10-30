/**
 * Contratos de la API UrbanAlert (Spring Boot).
 * Cada interfaz refleja un DTO de com.utp.urbanalert.dto; los tipos union reflejan los enums de model/.
 * Las fechas llegan como LocalDateTime ISO sin zona (ej. "2026-10-29T10:15:30.123").
 */

// ---------- Enums (model/*.java) ----------

export type NombreRol = 'ADMIN' | 'OPERADOR' | 'CIUDADANO';

export type EstadoIncidencia = 'REGISTRADA' | 'EN_PROCESO' | 'RESUELTA' | 'CERRADA';

export type PrioridadIncidencia = 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';

export type CategoriaIncidencia =
  | 'ALUMBRADO_PUBLICO'
  | 'PISTAS'
  | 'VEREDAS'
  | 'SEMAFOROS'
  | 'RESIDUOS'
  | 'SENALIZACION'
  | 'INFRAESTRUCTURA_PUBLICA';

/** Permisos de model/Permiso.java (el backend no los expone; ver core/auth/permisos.ts). */
export type Permiso =
  | 'INCIDENCIA_CREAR'
  | 'INCIDENCIA_VER'
  | 'INCIDENCIA_EDITAR'
  | 'INCIDENCIA_CAMBIAR_ESTADO'
  | 'INCIDENCIA_ASIGNAR_AREA'
  | 'INCIDENCIA_ELIMINAR'
  | 'CIUDADANO_GESTIONAR'
  | 'AREA_GESTIONAR'
  | 'REPORTE_VER';

// ---------- Auth ----------

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tipo: string;
  expiraEnSegundos: number;
  username: string;
  roles: NombreRol[];
}

export interface RegistroRequest {
  username: string;
  password: string;
  nombre: string;
  dni: string;
  email?: string | null;
  telefono?: string | null;
}

// ---------- Usuarios ----------

export interface UsuarioResponse {
  id: number;
  username: string;
  habilitado: boolean;
  roles: NombreRol[];
  ciudadanoId: number | null;
  areaId: number | null;
  areaNombre: string | null;
  fechaCreacion: string;
}

export interface CrearUsuarioRequest {
  username: string;
  password: string;
  rol: Exclude<NombreRol, 'CIUDADANO'>;
  areaId?: number | null;
}

// ---------- Ciudadanos ----------

export interface CiudadanoResponse {
  id: number;
  nombre: string;
  dni: string;
  email: string | null;
  telefono: string | null;
}

export interface CiudadanoRequest {
  nombre: string;
  dni: string;
  email?: string | null;
  telefono?: string | null;
}

export interface PerfilRequest {
  nombre: string;
  email?: string | null;
  telefono?: string | null;
}

// ---------- Areas ----------

export interface AreaResponse {
  id: number;
  nombre: string;
  descripcion: string | null;
}

export interface AreaRequest {
  nombre: string;
  descripcion?: string | null;
}

// ---------- Incidencias ----------

export interface IncidenciaResponse {
  id: number;
  titulo: string;
  descripcion: string;
  categoria: CategoriaIncidencia;
  prioridad: PrioridadIncidencia;
  estado: EstadoIncidencia;
  direccionReferencial: string | null;
  fechaReporte: string;
  fechaActualizacion: string | null;
  fechaResolucion: string | null;
  ciudadanoId: number;
  ciudadanoNombre: string;
  areaResponsableId: number | null;
  areaResponsableNombre: string | null;
}

export interface IncidenciaRequest {
  titulo: string;
  descripcion: string;
  categoria: CategoriaIncidencia;
  prioridad: PrioridadIncidencia;
  direccionReferencial?: string | null;
  /** Solo personal municipal; el servidor lo ignora si registra un CIUDADANO. */
  ciudadanoId?: number | null;
  /** Solo personal municipal; el servidor lo ignora si registra un CIUDADANO. */
  areaId?: number | null;
}

export interface IncidenciaActualizacionRequest {
  titulo: string;
  descripcion: string;
  categoria: CategoriaIncidencia;
  prioridad: PrioridadIncidencia;
  direccionReferencial?: string | null;
}

/** Query params de GET /api/incidencias (fechas yyyy-MM-dd, inclusivas). */
export interface IncidenciaFiltro {
  estado?: EstadoIncidencia | null;
  categoria?: CategoriaIncidencia | null;
  prioridad?: PrioridadIncidencia | null;
  areaId?: number | null;
  ciudadanoId?: number | null;
  desde?: string | null;
  hasta?: string | null;
}

export interface HistorialResponse {
  id: number;
  estadoAnterior: EstadoIncidencia | null;
  estadoNuevo: EstadoIncidencia;
  detalle: string;
  realizadoPor: string;
  fecha: string;
}

// ---------- Reportes ----------

export interface ResumenPublicoResponse {
  total: number;
  porEstado: Record<EstadoIncidencia, number>;
  porCategoria: Record<CategoriaIncidencia, number>;
}

export interface IndicadoresResponse {
  total: number;
  porEstado: Record<EstadoIncidencia, number>;
  porCategoria: Record<CategoriaIncidencia, number>;
  diasUmbral: number;
  pendientesAntiguas: number;
  tiempoPromedioResolucionHoras: number;
  tiempoPromedioResolucionHorasPorCategoria: Partial<Record<CategoriaIncidencia, number>>;
}

// ---------- Errores (GlobalExceptionHandler / ManejadorErroresSeguridad) ----------

export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  mensaje?: string;
  errores?: Record<string, string>;
}
