import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { IncidenciasApi } from '../../../core/api/apis';
import { ESTADOS } from '../../../core/models/catalogos';
import { leerError } from '../../../core/services/api-error';
import { ConfirmService } from '../../../core/services/confirm.service';
import { ToastService } from '../../../core/services/toast.service';
import { HistorialTimeline } from '../../../shared/components/historial-timeline';
import { Icon } from '../../../shared/components/icon';
import { IncidenciaFicha } from '../../../shared/components/incidencia-ficha';
import { Loading } from '../../../shared/components/ui';

/** Detalle y seguimiento: GET /api/incidencias/{id} + /historial. Editar/eliminar solo si REGISTRADA. */
@Component({
  selector: 'ua-portal-detalle',
  imports: [RouterLink, Icon, Loading, IncidenciaFicha, HistorialTimeline],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="back-link" routerLink="/portal/incidencias"><ua-icon name="arrow-left" [size]="16" />Mis incidencias</a>

    @if (incidencia.error()) {
      <div class="alert alert-error"><ua-icon name="alert" />{{ errorMsg() }}</div>
    } @else if (incidencia.value(); as i) {
      <div class="detail-actions">
        @if (i.estado === 'REGISTRADA') {
          <a class="btn btn-secondary" [routerLink]="['/portal/incidencias', i.id, 'editar']"><ua-icon name="pencil" />Editar</a>
          <button type="button" class="btn btn-danger-outline" (click)="eliminar()" [disabled]="eliminando()"><ua-icon name="trash" />Eliminar</button>
        }
      </div>

      <div class="detail-grid">
        <div class="detail-main">
          <ua-incidencia-ficha [incidencia]="i" />

          <section class="card">
            <div class="card-header"><h2>Progreso de la atención</h2></div>
            <ol class="stepper">
              @for (paso of pasos(); track paso.value) {
                <li [class.done]="paso.hecho" [class.current]="paso.actual">
                  <span class="step-dot"></span>
                  <span>{{ paso.label }}</span>
                </li>
              }
            </ol>
          </section>
        </div>

        <aside class="detail-side">
          <section class="card">
            <div class="card-header"><h2>Historial de seguimiento</h2></div>
            @if (historial.isLoading()) {
              <ua-loading />
            } @else {
              <ua-historial [items]="historial.value() ?? []" />
            }
          </section>
        </aside>
      </div>
    } @else {
      <ua-loading />
    }
  `,
})
export class PortalDetalle {
  readonly id = input.required<string>();

  private readonly api = inject(IncidenciasApi);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly incidencia = rxResource({ params: () => +this.id(), stream: ({ params }) => this.api.obtener(params) });
  protected readonly historial = rxResource({ params: () => +this.id(), stream: ({ params }) => this.api.historial(params) });
  protected readonly eliminando = signal(false);
  protected readonly errorMsg = computed(() => leerError(this.incidencia.error()).mensaje);

  protected readonly pasos = computed(() => {
    const estado = this.incidencia.value()?.estado;
    const indice = ESTADOS.findIndex((e) => e.value === estado);
    return ESTADOS.map((e, n) => ({ ...e, hecho: n < indice, actual: n === indice }));
  });

  protected async eliminar(): Promise<void> {
    const ok = await this.confirm.preguntar({
      titulo: 'Eliminar incidencia',
      mensaje: 'Se eliminará el reporte y su historial. Esta acción no se puede deshacer.',
      confirmar: 'Eliminar',
      peligro: true,
    });
    if (!ok) return;
    this.eliminando.set(true);
    this.api.eliminar(+this.id()).subscribe({
      next: () => {
        this.toast.success('Incidencia eliminada.');
        this.router.navigate(['/portal/incidencias']);
      },
      error: (e) => {
        this.toast.error(leerError(e).mensaje);
        this.eliminando.set(false);
      },
    });
  }
}
