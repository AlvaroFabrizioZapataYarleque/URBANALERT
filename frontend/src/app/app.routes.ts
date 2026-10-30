import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { SessionService } from './core/auth/session.service';
import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guards';
import { PublicLayout } from './layouts/public-layout/public-layout';
import { Shell, ShellConfig } from './layouts/shell/shell';

const PORTAL: ShellConfig = {
  contexto: 'Portal ciudadano',
  grupos: [
    {
      items: [
        { ruta: '/portal', etiqueta: 'Inicio', icono: 'dashboard', exacto: true },
        { ruta: '/portal/reportar', etiqueta: 'Reportar incidencia', icono: 'plus', exacto: true },
        { ruta: '/portal/incidencias', etiqueta: 'Mis incidencias', icono: 'list', exacto: false },
      ],
    },
    { titulo: 'Cuenta', items: [{ ruta: '/portal/perfil', etiqueta: 'Mi perfil', icono: 'user' }] },
  ],
};

const PANEL: ShellConfig = {
  contexto: 'Panel municipal',
  grupos: [
    {
      titulo: 'Operación',
      items: [
        { ruta: '/panel', etiqueta: 'Dashboard', icono: 'dashboard', exacto: true },
        { ruta: '/panel/incidencias', etiqueta: 'Bandeja de incidencias', icono: 'inbox' },
        { ruta: '/panel/registro', etiqueta: 'Registro presencial', icono: 'file' },
        { ruta: '/panel/reportes', etiqueta: 'Reportes', icono: 'chart', roles: ['ADMIN'] },
      ],
    },
    {
      titulo: 'Administración',
      items: [
        { ruta: '/panel/ciudadanos', etiqueta: 'Ciudadanos', icono: 'users' },
        { ruta: '/panel/areas', etiqueta: 'Áreas responsables', icono: 'building', roles: ['ADMIN'] },
        { ruta: '/panel/usuarios', etiqueta: 'Usuarios', icono: 'shield', roles: ['ADMIN'] },
      ],
    },
  ],
};

export const routes: Routes = [
  // La raiz lleva al inicio del rol (o al login si no hay sesion).
  { path: '', pathMatch: 'full', redirectTo: () => inject(SessionService).inicio() },
  {
    path: '',
    component: PublicLayout,
    canActivate: [guestGuard],
    children: [
      { path: 'login', title: 'Iniciar sesión · UrbanAlert', loadComponent: () => import('./features/auth/login/login').then((m) => m.Login) },
      { path: 'registro', title: 'Registro · UrbanAlert', loadComponent: () => import('./features/auth/registro/registro').then((m) => m.Registro) },
    ],
  },
  {
    path: 'portal',
    component: Shell,
    data: { shell: PORTAL },
    canActivate: [authGuard, roleGuard('CIUDADANO')],
    loadChildren: () => import('./features/portal/portal.routes').then((m) => m.PORTAL_ROUTES),
  },
  {
    path: 'panel',
    component: Shell,
    data: { shell: PANEL },
    canActivate: [authGuard, roleGuard('ADMIN', 'OPERADOR')],
    loadChildren: () => import('./features/panel/panel.routes').then((m) => m.PANEL_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
