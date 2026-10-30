import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { EstadoIncidencia, PrioridadIncidencia } from '../../core/models/api.models';
import { LabelPipe } from '../pipes/label.pipe';

@Component({
  selector: 'ua-estado-badge',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge" [class]="'badge estado-' + estado()">
    <span class="badge-dot"></span>{{ estado() | label: 'estado' }}
  </span>`,
})
export class EstadoBadge {
  readonly estado = input.required<EstadoIncidencia>();
}

@Component({
  selector: 'ua-prioridad-badge',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="'prioridad prioridad-' + prioridad()">
    <span class="prioridad-bars"><i></i><i></i><i></i><i></i></span>{{ prioridad() | label: 'prioridad' }}
  </span>`,
})
export class PrioridadBadge {
  readonly prioridad = input.required<PrioridadIncidencia>();
}
