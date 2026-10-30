import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AreaRequest,
  AreaResponse,
  CiudadanoRequest,
  CiudadanoResponse,
  CrearUsuarioRequest,
  EstadoIncidencia,
  HistorialResponse,
  IncidenciaActualizacionRequest,
  IncidenciaFiltro,
  IncidenciaRequest,
  IncidenciaResponse,
  IndicadoresResponse,
  PerfilRequest,
  ResumenPublicoResponse,
  UsuarioResponse,
} from '../models/api.models';

const API = environment.apiUrl;

/** Convierte un objeto de filtros en query params, omitiendo los vacios. */
function params(filtro: object): HttpParams {
  let p = new HttpParams();
  for (const [clave, valor] of Object.entries(filtro)) {
    if (valor !== null && valor !== undefined && valor !== '') p = p.set(clave, String(valor));
  }
  return p;
}

/** IncidenciaController: /api/incidencias */
@Injectable({ providedIn: 'root' })
export class IncidenciasApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API}/incidencias`;

  listar(filtro: IncidenciaFiltro = {}): Observable<IncidenciaResponse[]> {
    return this.http.get<IncidenciaResponse[]>(this.url, { params: params(filtro) });
  }

  obtener(id: number): Observable<IncidenciaResponse> {
    return this.http.get<IncidenciaResponse>(`${this.url}/${id}`);
  }

  historial(id: number): Observable<HistorialResponse[]> {
    return this.http.get<HistorialResponse[]>(`${this.url}/${id}/historial`);
  }

  registrar(datos: IncidenciaRequest): Observable<IncidenciaResponse> {
    return this.http.post<IncidenciaResponse>(this.url, datos);
  }

  actualizar(id: number, datos: IncidenciaActualizacionRequest): Observable<IncidenciaResponse> {
    return this.http.put<IncidenciaResponse>(`${this.url}/${id}`, datos);
  }

  cambiarEstado(id: number, estado: EstadoIncidencia): Observable<IncidenciaResponse> {
    return this.http.put<IncidenciaResponse>(`${this.url}/${id}/estado`, { estado });
  }

  asignarArea(id: number, areaId: number): Observable<IncidenciaResponse> {
    return this.http.put<IncidenciaResponse>(`${this.url}/${id}/area`, { areaId });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

/** AreaController: /api/areas */
@Injectable({ providedIn: 'root' })
export class AreasApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API}/areas`;

  listar(): Observable<AreaResponse[]> {
    return this.http.get<AreaResponse[]>(this.url);
  }

  crear(datos: AreaRequest): Observable<AreaResponse> {
    return this.http.post<AreaResponse>(this.url, datos);
  }

  actualizar(id: number, datos: AreaRequest): Observable<AreaResponse> {
    return this.http.put<AreaResponse>(`${this.url}/${id}`, datos);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

/** CiudadanoController: /api/ciudadanos */
@Injectable({ providedIn: 'root' })
export class CiudadanosApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API}/ciudadanos`;

  listar(): Observable<CiudadanoResponse[]> {
    return this.http.get<CiudadanoResponse[]>(this.url);
  }

  obtener(id: number): Observable<CiudadanoResponse> {
    return this.http.get<CiudadanoResponse>(`${this.url}/${id}`);
  }

  registrar(datos: CiudadanoRequest): Observable<CiudadanoResponse> {
    return this.http.post<CiudadanoResponse>(this.url, datos);
  }

  miPerfil(): Observable<CiudadanoResponse> {
    return this.http.get<CiudadanoResponse>(`${this.url}/me`);
  }

  actualizarMiPerfil(datos: PerfilRequest): Observable<CiudadanoResponse> {
    return this.http.put<CiudadanoResponse>(`${this.url}/me`, datos);
  }
}

/** UsuarioController: /api/usuarios (solo ADMIN) */
@Injectable({ providedIn: 'root' })
export class UsuariosApi {
  private readonly http = inject(HttpClient);
  private readonly url = `${API}/usuarios`;

  listar(): Observable<UsuarioResponse[]> {
    return this.http.get<UsuarioResponse[]>(this.url);
  }

  crear(datos: CrearUsuarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(this.url, datos);
  }

  cambiarHabilitado(id: number, habilitado: boolean): Observable<UsuarioResponse> {
    return this.http.put<UsuarioResponse>(`${this.url}/${id}/habilitado`, { habilitado });
  }
}

/** ReporteController (/api/reportes, solo ADMIN) y PublicoController (/api/publico). */
@Injectable({ providedIn: 'root' })
export class ReportesApi {
  private readonly http = inject(HttpClient);

  indicadores(dias: number): Observable<IndicadoresResponse> {
    return this.http.get<IndicadoresResponse>(`${API}/reportes/indicadores`, { params: params({ dias }) });
  }

  pendientes(dias: number): Observable<IncidenciaResponse[]> {
    return this.http.get<IncidenciaResponse[]>(`${API}/reportes/pendientes`, { params: params({ dias }) });
  }

  resumenPublico(): Observable<ResumenPublicoResponse> {
    return this.http.get<ResumenPublicoResponse>(`${API}/publico/resumen`);
  }
}
