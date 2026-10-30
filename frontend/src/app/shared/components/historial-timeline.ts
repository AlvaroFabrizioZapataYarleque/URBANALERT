import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { HistorialResponse } from '../../core/models/api.models';
import { LabelPipe } from '../pipes/label.pipe';

/** Historial de seguimiento (lo genera el backend en cada alta, edicion, derivacion y cambio de estado). */
@Component({
  selector: 'ua-historial',
  imports: [DatePipe, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (items().length === 0) {
      <p class="muted small">Aún no hay movimientos registrados.</p>
    } @else {
      <ol class="timeline">
        @for (h of items(); track h.id; let ultimo = $last) {
          <li class="timeline-item" [class.is-last]="ultimo">
            <span class="timeline-dot" [class]="'timeline-dot dot-' + h.estadoNuevo"></span>
            <div class="timeline-body">
              <div class="timeline-head">
                <strong>{{ titulo(h) }}</strong>
                <time class="muted small">{{ h.fecha | date: 'dd/MM/yyyy HH:mm' }}</time>
              </div>
              <p>{{ h.detalle }}</p>
              <span class="muted small">
                Por {{ h.realizadoPor }} · Estado: {{ h.estadoNuevo | label: 'estado' }}
              </span>
            </div>
          </li>
        }
      </ol>
    }
  `,
})
export class HistorialTimeline {
  readonly items = input.required<HistorialResponse[]>();

  protected titulo(h: HistorialResponse): string {
    if (h.estadoAnterior === null) return 'Reporte registrado';
    if (h.estadoAnterior !== h.estadoNuevo) return 'Cambio de estado';
    return h.detalle.startsWith('Area derivada') ? 'Derivación de área' : 'Actualización de datos';
  }
}
