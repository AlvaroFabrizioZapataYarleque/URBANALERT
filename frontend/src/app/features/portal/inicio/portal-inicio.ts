import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { CiudadanosApi, IncidenciasApi } from '../../../core/api/apis';
import { SessionService } from '../../../core/auth/session.service';
import { leerError } from '../../../core/services/api-error';
import { EstadoBadge } from '../../../shared/components/badges';
import { Icon } from '../../../shared/components/icon';
import { EmptyState, Loading, StatCard } from '../../../shared/components/ui';
import { LabelPipe } from '../../../shared/pipes/label.pipe';

/** Dashboard del ciudadano: cifras calculadas sobre SUS incidencias (GET /api/incidencias, alcance por rol). */
@Component({
  selector: 'ua-portal-inicio',
  imports: [RouterLink, DatePipe, Icon, StatCard, EmptyState, Loading, EstadoBadge, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-header">
      <div>
        <h1>Hola, {{ nombre() }}</h1>
        <p>Desde aquí puede reportar problemas en su barrio y seguir su atención.</p>
      </div>
      <div class="page-actions">
        <a class="btn btn-primary" routerLink="/portal/reportar"><ua-icon name="plus" />Reportar incidencia</a>
      </div>
    </div>

    @if (incidencias.error()) {
      <div class="alert alert-error"><ua-icon name="alert" />{{ errorMsg() }}</div>
    }

    <div class="stats-grid">
      <ua-stat-card etiqueta="Total reportadas" [valor]="cifras().total" icono="list" tono="slate" />
      <ua-stat-card etiqueta="Pendientes" [valor]="cifras().registradas" icono="clock" tono="blue" detalle="Registradas, por atender" />
      <ua-stat-card etiqueta="En proceso" [valor]="cifras().enProceso" icono="refresh" tono="amber" detalle="Asignadas a un área" />
      <ua-stat-card etiqueta="Resueltas" [valor]="cifras().resueltas" icono="check" tono="green" detalle="Resueltas o cerradas" />
    </div>

    <section class="card">
      <div class="card-header">
        <h2>Últimos reportes</h2>
        <a routerLink="/portal/incidencias" class="link">Ver todos <ua-icon name="arrow-right" [size]="14" /></a>
      </div>
      @if (incidencias.isLoading()) {
        <ua-loading />
      } @else if (recientes().length === 0) {
        <ua-empty titulo="Todavía no tiene reportes" mensaje="Cuando registre una incidencia aparecerá aquí con su estado.">
          <a class="btn btn-secondary" routerLink="/portal/reportar">Reportar mi primera incidencia</a>
        </ua-empty>
      } @else {
        <ul class="list">
          @for (i of recientes(); track i.id) {
            <li>
              <a class="list-item" [routerLink]="['/portal/incidencias', i.id]">
                <div class="list-main">
                  <span class="code">#{{ i.id }}</span>
                  <strong>{{ i.titulo }}</strong>
                  <span class="muted small">{{ i.categoria | label: 'categoria' }} · {{ i.fechaReporte | date: 'dd/MM/yyyy' }}</span>
                </div>
                <ua-estado-badge [estado]="i.estado" />
              </a>
            </li>
          }
        </ul>
      }
    </section>
  `,
})
export class PortalInicio {
  private readonly api = inject(IncidenciasApi);
  private readonly ciudadanos = inject(CiudadanosApi);
  private readonly session = inject(SessionService);

  protected readonly incidencias = rxResource({ stream: () => this.api.listar() });
  private readonly perfil = rxResource({ stream: () => this.ciudadanos.miPerfil() });

  protected readonly nombre = computed(() => this.perfil.value()?.nombre.split(' ')[0] ?? this.session.username());
  protected readonly errorMsg = computed(() => leerError(this.incidencias.error()).mensaje);

  protected readonly cifras = computed(() => {
    const lista = this.incidencias.value() ?? [];
    const contar = (...estados: string[]) => lista.filter((i) => estados.includes(i.estado)).length;
    return {
      total: lista.length,
      registradas: contar('REGISTRADA'),
      enProceso: contar('EN_PROCESO'),
      resueltas: contar('RESUELTA', 'CERRADA'),
    };
  });

  // La API ya devuelve ordenado por fechaReporte DESC.
  protected readonly recientes = computed(() => (this.incidencias.value() ?? []).slice(0, 5));
}
