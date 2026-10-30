import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegistroRequest, UsuarioResponse } from '../models/api.models';
import { SessionService } from './session.service';

/** AuthController: /api/auth (publico). */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionService);
  private readonly url = `${environment.apiUrl}/auth`;

  login(credenciales: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.url}/login`, credenciales)
      .pipe(tap((respuesta) => this.session.iniciar(respuesta)));
  }

  registrar(datos: RegistroRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${this.url}/register`, datos);
  }
}
