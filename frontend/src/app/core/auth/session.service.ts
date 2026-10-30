import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthResponse, NombreRol, Permiso } from '../models/api.models';
import { permisosDe } from './permisos';

export interface Sesion {
  token: string;
  username: string;
  roles: NombreRol[];
  /** Epoch en ms en que vence el token (segun expiraEnSegundos del login). */
  expiraEn: number;
}

export type MotivoCierre = 'manual' | 'expirada';

const CLAVE = 'ua.session';

/** Estado de la sesion JWT. El backend es stateless: la sesion solo existe en el cliente. */
@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly router = inject(Router);
  private readonly estado = signal<Sesion | null>(this.leer());
  private temporizador: ReturnType<typeof setTimeout> | null = null;

  readonly sesion = this.estado.asReadonly();
  readonly autenticado = computed(() => this.estado() !== null);
  readonly username = computed(() => this.estado()?.username ?? '');
  readonly roles = computed(() => this.estado()?.roles ?? []);
  readonly rolPrincipal = computed<NombreRol | null>(() => {
    const roles = this.roles();
    return roles.includes('ADMIN') ? 'ADMIN' : roles.includes('OPERADOR') ? 'OPERADOR' : roles[0] ?? null;
  });
  readonly permisos = computed(() => permisosDe(this.roles()));
  readonly esPersonal = computed(() => this.roles().some((r) => r === 'ADMIN' || r === 'OPERADOR'));

  constructor() {
    this.programarExpiracion();
  }

  iniciar(respuesta: AuthResponse): void {
    const sesion: Sesion = {
      token: respuesta.token,
      username: respuesta.username,
      roles: respuesta.roles,
      expiraEn: Date.now() + respuesta.expiraEnSegundos * 1000,
    };
    try {
      localStorage.setItem(CLAVE, JSON.stringify(sesion));
    } catch {
      /* almacenamiento no disponible: la sesion vive solo en memoria */
    }
    this.estado.set(sesion);
    this.programarExpiracion();
  }

  cerrar(motivo: MotivoCierre = 'manual'): void {
    if (this.temporizador) clearTimeout(this.temporizador);
    try {
      localStorage.removeItem(CLAVE);
    } catch {
      /* sin almacenamiento */
    }
    const teniaSesion = this.estado() !== null;
    this.estado.set(null);
    if (teniaSesion || motivo === 'manual') {
      this.router.navigate(['/login'], motivo === 'expirada' ? { queryParams: { motivo } } : {});
    }
  }

  token(): string | null {
    return this.estado()?.token ?? null;
  }

  tieneRol(...roles: NombreRol[]): boolean {
    return roles.some((r) => this.roles().includes(r));
  }

  puede(permiso: Permiso): boolean {
    return this.permisos().has(permiso);
  }

  /** Pagina de inicio segun el rol: CIUDADANO al portal, personal municipal al panel. */
  inicio(): string {
    if (!this.autenticado()) return '/login';
    return this.esPersonal() ? '/panel' : '/portal';
  }

  private leer(): Sesion | null {
    try {
      const crudo = localStorage.getItem(CLAVE);
      if (!crudo) return null;
      const sesion = JSON.parse(crudo) as Sesion;
      if (!sesion.token || !Array.isArray(sesion.roles) || sesion.expiraEn <= Date.now()) {
        localStorage.removeItem(CLAVE);
        return null;
      }
      return sesion;
    } catch {
      return null;
    }
  }

  private programarExpiracion(): void {
    if (this.temporizador) clearTimeout(this.temporizador);
    const sesion = this.estado();
    if (!sesion) return;
    // setTimeout admite como maximo ~24.8 dias.
    const restante = Math.min(sesion.expiraEn - Date.now(), 2_147_000_000);
    this.temporizador = setTimeout(() => this.cerrar('expirada'), Math.max(restante, 0));
  }
}
