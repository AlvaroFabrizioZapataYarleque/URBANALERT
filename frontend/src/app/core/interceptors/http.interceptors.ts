import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SessionService } from '../auth/session.service';

const esApi = (url: string) => url.startsWith(environment.apiUrl);
const esAuth = (url: string) => url.startsWith(`${environment.apiUrl}/auth/`);

/** Adjunta "Authorization: Bearer <token>" a las solicitudes hacia la API (JwtAuthenticationFilter). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(SessionService).token();
  if (!token || !esApi(req.url) || esAuth(req.url)) return next(req);
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};

/**
 * Un 401 en una ruta protegida significa token vencido o invalido (ManejadorErroresSeguridad):
 * se cierra la sesion y se vuelve al login. El 401 del propio login (credenciales) se deja pasar.
 */
export const sessionExpiryInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(SessionService);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && esApi(req.url) && !esAuth(req.url)) {
        session.cerrar('expirada');
      }
      return throwError(() => error);
    }),
  );
};
