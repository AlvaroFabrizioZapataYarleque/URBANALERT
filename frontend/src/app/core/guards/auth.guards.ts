import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from '../auth/session.service';
import { NombreRol } from '../models/api.models';

/** Exige sesion activa; si no, envia al login conservando la ruta solicitada. */
export const authGuard: CanActivateFn = (_route, state) => {
  const session = inject(SessionService);
  if (session.autenticado()) return true;
  return inject(Router).createUrlTree(['/login'], { queryParams: { volver: state.url } });
};

/** Para login/registro: un usuario con sesion va directo a su inicio. */
export const guestGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  return session.autenticado() ? inject(Router).parseUrl(session.inicio()) : true;
};

/** Restringe una ruta a ciertos roles; si no corresponde, redirige al inicio de su rol. */
export function roleGuard(...roles: NombreRol[]): CanActivateFn {
  return () => {
    const session = inject(SessionService);
    if (!session.autenticado()) return inject(Router).parseUrl('/login');
    return session.tieneRol(...roles) ? true : inject(Router).parseUrl(session.inicio());
  };
}
