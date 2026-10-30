import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/auth.guards';

export const PANEL_ROUTES: Routes = [
  { path: '', title: 'Dashboard · UrbanAlert', loadComponent: () => import('./inicio/panel-inicio').then((m) => m.PanelInicio) },
  { path: 'incidencias', title: 'Bandeja de incidencias · UrbanAlert', loadComponent: () => import('./bandeja/bandeja').then((m) => m.Bandeja) },
  { path: 'incidencias/:id', title: 'Gestión de incidencia · UrbanAlert', loadComponent: () => import('./gestion/gestion').then((m) => m.Gestion) },
  { path: 'registro', title: 'Registro presencial · UrbanAlert', loadComponent: () => import('./registro-presencial/registro-presencial').then((m) => m.RegistroPresencial) },
  { path: 'ciudadanos', title: 'Ciudadanos · UrbanAlert', loadComponent: () => import('./ciudadanos/ciudadanos').then((m) => m.Ciudadanos) },
  { path: 'areas', title: 'Áreas responsables · UrbanAlert', canActivate: [roleGuard('ADMIN')], loadComponent: () => import('./areas/areas').then((m) => m.Areas) },
  { path: 'usuarios', title: 'Usuarios · UrbanAlert', canActivate: [roleGuard('ADMIN')], loadComponent: () => import('./usuarios/usuarios').then((m) => m.Usuarios) },
  { path: 'reportes', title: 'Reportes · UrbanAlert', canActivate: [roleGuard('ADMIN')], loadComponent: () => import('./reportes/reportes').then((m) => m.Reportes) },
];
