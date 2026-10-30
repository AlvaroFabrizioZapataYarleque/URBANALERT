import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IncidenciaResponse } from '../../core/models/api.models';
import { LabelPipe } from '../pipes/label.pipe';
import { EstadoBadge, PrioridadBadge } from './badges';
import { Icon } from './icon';

/** Ficha con los datos completos de una incidencia (IncidenciaResponse). */
@Component({
  selector: 'ua-incidencia-ficha',
  imports: [DatePipe, LabelPipe, Icon, EstadoBadge, PrioridadBadge],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let i = incidencia();
    <section class="card">
      <div class="ficha-head">
        <div>
          <span class="code">Incidencia #{{ i.id }}</span>
          <h2>{{ i.titulo }}</h2>
        </div>
        <ua-estado-badge [estado]="i.estado" />
      </div>
      <p class="ficha-desc">{{ i.descripcion }}</p>

      <dl class="meta-grid">
        <div>
          <dt><ua-icon name="layers" [size]="14" />Categoría</dt>
          <dd>{{ i.categoria | label: 'categoria' }}</dd>
        </div>
        <div>
          <dt><ua-icon name="flag" [size]="14" />Prioridad</dt>
          <dd><ua-prioridad-badge [prioridad]="i.prioridad" /></dd>
        </div>
        <div>
          <dt><ua-icon name="pin" [size]="14" />Ubicación</dt>
          <dd>{{ i.direccionReferencial || 'Sin dirección registrada' }}</dd>
        </div>
        <div>
          <dt><ua-icon name="building" [size]="14" />Área responsable</dt>
          <dd>{{ i.areaResponsableNombre || 'Pendiente de asignación' }}</dd>
        </div>
        @if (mostrarCiudadano()) {
          <div>
            <dt><ua-icon name="user" [size]="14" />Reportado por</dt>
            <dd>{{ i.ciudadanoNombre }}</dd>
          </div>
        }
        <div>
          <dt><ua-icon name="calendar" [size]="14" />Fecha de registro</dt>
          <dd>{{ i.fechaReporte | date: 'dd/MM/yyyy HH:mm' }}</dd>
        </div>
        <div>
          <dt><ua-icon name="clock" [size]="14" />Última actualización</dt>
          <dd>{{ i.fechaActualizacion ? (i.fechaActualizacion | date: 'dd/MM/yyyy HH:mm') : '—' }}</dd>
        </div>
        @if (i.fechaResolucion) {
          <div>
            <dt><ua-icon name="check" [size]="14" />Fecha de resolución</dt>
            <dd>{{ i.fechaResolucion | date: 'dd/MM/yyyy HH:mm' }}</dd>
          </div>
        }
      </dl>
    </section>
  `,
})
export class IncidenciaFicha {
  readonly incidencia = input.required<IncidenciaResponse>();
  readonly mostrarCiudadano = input(false);
}
