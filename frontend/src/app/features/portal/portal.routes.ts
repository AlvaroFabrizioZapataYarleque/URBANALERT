import { Routes } from '@angular/router';

export const PORTAL_ROUTES: Routes = [
  { path: '', title: 'Inicio · UrbanAlert', loadComponent: () => import('./inicio/portal-inicio').then((m) => m.PortalInicio) },
  { path: 'incidencias', title: 'Mis incidencias · UrbanAlert', loadComponent: () => import('./mis-incidencias/mis-incidencias').then((m) => m.MisIncidencias) },
  { path: 'reportar', title: 'Reportar incidencia · UrbanAlert', loadComponent: () => import('./incidencia-form/incidencia-form').then((m) => m.PortalIncidenciaForm) },
  { path: 'incidencias/:id', title: 'Detalle de incidencia · UrbanAlert', loadComponent: () => import('./detalle/portal-detalle').then((m) => m.PortalDetalle) },
  { path: 'incidencias/:id/editar', title: 'Editar incidencia · UrbanAlert', loadComponent: () => import('./incidencia-form/incidencia-form').then((m) => m.PortalIncidenciaForm) },
  { path: 'perfil', title: 'Mi perfil · UrbanAlert', loadComponent: () => import('./perfil/perfil').then((m) => m.Perfil) },
];
