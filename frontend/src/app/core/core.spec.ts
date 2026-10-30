import { HttpErrorResponse, HttpRequest, HttpResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { createTableState } from '../shared/utils/table-state';
import { permisosDe } from './auth/permisos';
import { SessionService } from './auth/session.service';
import { authGuard, roleGuard } from './guards/auth.guards';
import { authInterceptor, sessionExpiryInterceptor } from './interceptors/http.interceptors';
import { AuthResponse } from './models/api.models';
import { TRANSICIONES } from './models/catalogos';
import { aplicarErroresServidor, leerError } from './services/api-error';

@Component({ template: '' })
class Vacio {}
const RUTAS = [{ path: 'login', component: Vacio }];

const login = (roles: AuthResponse['roles']): AuthResponse => ({
  token: 'jwt.de.prueba',
  tipo: 'Bearer',
  expiraEnSegundos: 7200,
  username: 'usuario',
  roles,
});

describe('SessionService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter(RUTAS)] });
  });

  it('guarda la sesion y deriva el inicio segun el rol', () => {
    const s = TestBed.inject(SessionService);
    s.iniciar(login(['CIUDADANO']));
    expect(s.autenticado()).toBe(true);
    expect(s.inicio()).toBe('/portal');
    expect(JSON.parse(localStorage.getItem('ua.session')!).token).toBe('jwt.de.prueba');

    s.iniciar(login(['OPERADOR']));
    expect(s.inicio()).toBe('/panel');
    expect(s.puede('INCIDENCIA_ASIGNAR_AREA')).toBe(false);
  });

  it('descarta una sesion almacenada que ya vencio', () => {
    localStorage.setItem('ua.session', JSON.stringify({ token: 't', username: 'u', roles: ['ADMIN'], expiraEn: Date.now() - 1 }));
    expect(TestBed.inject(SessionService).autenticado()).toBe(false);
    expect(localStorage.getItem('ua.session')).toBeNull();
  });

  it('cerrar() limpia el almacenamiento', () => {
    const s = TestBed.inject(SessionService);
    s.iniciar(login(['ADMIN']));
    s.cerrar('expirada');
    expect(s.autenticado()).toBe(false);
    expect(localStorage.getItem('ua.session')).toBeNull();
  });
});

describe('permisos y catalogos', () => {
  it('replica la tabla de DataLoader', () => {
    expect([...permisosDe(['ADMIN'])]).toHaveLength(9);
    expect(permisosDe(['OPERADOR']).has('INCIDENCIA_CAMBIAR_ESTADO')).toBe(true);
    expect(permisosDe(['CIUDADANO']).has('INCIDENCIA_CAMBIAR_ESTADO')).toBe(false);
  });

  it('refleja EstadoIncidencia.puedePasarA()', () => {
    expect(TRANSICIONES.REGISTRADA).toEqual(['EN_PROCESO']);
    expect(TRANSICIONES.RESUELTA).toContain('EN_PROCESO');
    expect(TRANSICIONES.CERRADA).toEqual([]);
  });
});

describe('guards', () => {
  const ejecutar = (guard: typeof authGuard) =>
    TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, { url: '/panel/usuarios' } as RouterStateSnapshot));

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter(RUTAS)] });
  });

  it('authGuard envia al login sin sesion', () => {
    const r = ejecutar(authGuard) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(r)).toBe('/login?volver=%2Fpanel%2Fusuarios');
  });

  it('roleGuard redirige a un ciudadano fuera del panel', () => {
    TestBed.inject(SessionService).iniciar(login(['CIUDADANO']));
    const r = ejecutar(roleGuard('ADMIN', 'OPERADOR')) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(r)).toBe('/portal');
  });

  it('roleGuard permite al rol correcto', () => {
    TestBed.inject(SessionService).iniciar(login(['ADMIN']));
    expect(ejecutar(roleGuard('ADMIN'))).toBe(true);
  });
});

describe('interceptores', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter(RUTAS)] });
    TestBed.inject(SessionService).iniciar(login(['ADMIN']));
  });

  it('agrega el Bearer solo a la API protegida', async () => {
    const vistas: HttpRequest<unknown>[] = [];
    const next = (r: HttpRequest<unknown>) => {
      vistas.push(r);
      return of(new HttpResponse({ status: 200 }));
    };
    await TestBed.runInInjectionContext(() => firstValueFrom(authInterceptor(new HttpRequest('GET', `${environment.apiUrl}/incidencias`), next)));
    await TestBed.runInInjectionContext(() => firstValueFrom(authInterceptor(new HttpRequest('POST', `${environment.apiUrl}/auth/login`, {}), next)));
    expect(vistas[0].headers.get('Authorization')).toBe('Bearer jwt.de.prueba');
    expect(vistas[1].headers.has('Authorization')).toBe(false);
  });

  it('un 401 en ruta protegida cierra la sesion', async () => {
    const s = TestBed.inject(SessionService);
    const next = () => throwError(() => new HttpErrorResponse({ status: 401 }));
    await expect(
      TestBed.runInInjectionContext(() => firstValueFrom(sessionExpiryInterceptor(new HttpRequest('GET', `${environment.apiUrl}/incidencias`), next))),
    ).rejects.toBeInstanceOf(HttpErrorResponse);
    expect(s.autenticado()).toBe(false);
  });
});

describe('api-error', () => {
  it('lee el formato { mensaje } del GlobalExceptionHandler', () => {
    const e = new HttpErrorResponse({ status: 409, error: { status: 409, mensaje: 'Ya existe un ciudadano registrado con DNI 1' } });
    expect(leerError(e)).toEqual({ status: 409, mensaje: 'Ya existe un ciudadano registrado con DNI 1', campos: {} });
  });

  it('aplica los errores { errores } de Bean Validation al formulario', () => {
    const e = leerError(new HttpErrorResponse({ status: 400, error: { status: 400, errores: { dni: 'El DNI debe tener 8 digitos' } } }));
    const form = new FormGroup({ dni: new FormControl('1') });
    aplicarErroresServidor(form, e.campos);
    expect(form.controls.dni.errors?.['server']).toBe('El DNI debe tener 8 digitos');
    expect(e.mensaje).toBe('Revise los campos marcados.');
  });

  it('detecta servidor caido (status 0)', () => {
    expect(leerError(new HttpErrorResponse({ status: 0 })).mensaje).toContain('No se pudo conectar');
  });
});

describe('createTableState', () => {
  it('busca sin tildes, ordena y pagina', () => {
    const datos = signal(Array.from({ length: 23 }, (_, i) => ({ id: i + 1, nombre: i === 4 ? 'Señalización' : `Fila ${i + 1}` })));
    const t = createTableState(datos, {
      buscarEn: (f) => [f.nombre],
      ordenarPor: { id: (f) => f.id },
      ordenInicial: 'id',
      tamano: 10,
    });
    expect(t.visibles()[0].id).toBe(23);
    t.irA(3);
    expect(t.visibles()).toHaveLength(3);
    t.buscar('senal');
    expect(t.total()).toBe(1);
    expect(t.pagina()).toBe(1);
  });
});
