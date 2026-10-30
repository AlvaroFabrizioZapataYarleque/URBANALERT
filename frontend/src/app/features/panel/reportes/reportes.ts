import { DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ReportesApi } from '../../../core/api/apis';
import { CATEGORIAS, etiqueta } from '../../../core/models/catalogos';
import { leerError } from '../../../core/services/api-error';
import { EstadoBadge, PrioridadBadge } from '../../../shared/components/badges';
import { Icon } from '../../../shared/components/icon';
import { EmptyState, Loading, PageHeader, StatCard } from '../../../shared/components/ui';
import { LabelPipe } from '../../../shared/pipes/label.pipe';
import { diasDesde } from '../../../shared/utils/agregados';

/** GET /api/reportes/indicadores y /api/reportes/pendientes (REPORTE_VER: ADMIN). */
@Component({
  selector: 'ua-reportes',
  imports: [RouterLink, DatePipe, DecimalPipe, PercentPipe, Icon, PageHeader, StatCard, EmptyState, Loading, EstadoBadge, PrioridadBadge, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ua-page-header titulo="Reportes" descripcion="Incidencias vencidas y tiempos de resolución.">
      <label class="inline-field small">
        Vencidas con más de
        <select class="input input-sm" [value]="dias()" (change)="dias.set(+$any($event.target).value)">
          @for (d of opcionesDias; track d) {
            <option [value]="d" [selected]="d === dias()">{{ d }} días</option>
          }
        </select>
      </label>
      <button type="button" class="btn btn-secondary" (click)="exportar()" [disabled]="!(pendientes.value()?.length)"><ua-icon name="file" />Exportar CSV</button>
    </ua-page-header>

    @if (error()) {
      <div class="alert alert-error"><ua-icon name="alert" />{{ error() }}</div>
    }

    @if (indicadores.value(); as ind) {
      <div class="stats-grid">
        <ua-stat-card etiqueta="Total de incidencias" [valor]="ind.total" icono="list" tono="slate" />
        <ua-stat-card etiqueta="Vencidas" [valor]="ind.pendientesAntiguas" icono="alert" tono="red" [detalle]="'Más de ' + ind.diasUmbral + ' días sin resolver'" />
        <ua-stat-card etiqueta="Tiempo promedio de resolución" [valor]="(ind.tiempoPromedioResolucionHoras | number: '1.0-1') + ' h'" icono="clock" tono="blue" />
        <ua-stat-card etiqueta="Resueltas o cerradas" [valor]="ind.porEstado.RESUELTA + ind.porEstado.CERRADA" icono="check" tono="green" />
      </div>

      <section class="card card-flush">
        <div class="card-header card-header-pad"><h2>Desempeño por categoría</h2></div>
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Categoría</th>
                <th class="num">Reportadas</th>
                <th class="num">Participación</th>
                <th class="num">Tiempo promedio de resolución</th>
              </tr>
            </thead>
            <tbody>
              @for (c of categorias; track c.value) {
                <tr>
                  <td class="cell-strong">{{ c.label }}</td>
                  <td class="num">{{ ind.porCategoria[c.value] }}</td>
                  <td class="num">{{ (ind.total ? ind.porCategoria[c.value] / ind.total : 0) | percent }}</td>
                  <td class="num">
                    @if (ind.tiempoPromedioResolucionHorasPorCategoria[c.value]; as h) {
                      {{ h | number: '1.0-1' }} h
                    } @else {
                      <span class="muted">Sin resueltas</span>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    }

    <section class="card card-flush">
      <div class="card-header card-header-pad">
        <div>
          <h2>Incidencias vencidas</h2>
          <p class="muted small">Registradas o en proceso, reportadas hace más de {{ dias() }} días.</p>
        </div>
      </div>
      @if (pendientes.isLoading() && !pendientes.hasValue()) {
        <ua-loading />
      } @else if ((pendientes.value() ?? []).length === 0) {
        <ua-empty titulo="Sin incidencias vencidas" mensaje="Todas las pendientes están dentro del plazo." icono="check" />
      } @else {
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Código</th>
                <th>Incidencia</th>
                <th>Ciudadano</th>
                <th>Área</th>
                <th>Reportada</th>
                <th class="num">Días</th>
                <th>Prioridad</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (i of pendientes.value(); track i.id) {
                <tr class="row-link" [routerLink]="['/panel/incidencias', i.id]">
                  <td class="code">#{{ i.id }}</td>
                  <td>
                    <span class="cell-strong">{{ i.titulo }}</span>
                    <span class="cell-sub">{{ i.categoria | label: 'categoria' }}</span>
                  </td>
                  <td>{{ i.ciudadanoNombre }}</td>
                  <td>{{ i.areaResponsableNombre || 'Sin asignar' }}</td>
                  <td class="nowrap">{{ i.fechaReporte | date: 'dd/MM/yyyy' }}</td>
                  <td class="num"><strong>{{ antiguedad(i.fechaReporte) }}</strong></td>
                  <td><ua-prioridad-badge [prioridad]="i.prioridad" /></td>
                  <td><ua-estado-badge [estado]="i.estado" /></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
})
export class Reportes {
  private readonly api = inject(ReportesApi);
  protected readonly categorias = CATEGORIAS;
  protected readonly opcionesDias = [3, 7, 15, 30, 60];
  protected readonly dias = signal(7);

  protected readonly indicadores = rxResource({ params: () => this.dias(), stream: ({ params }) => this.api.indicadores(params) });
  protected readonly pendientes = rxResource({ params: () => this.dias(), stream: ({ params }) => this.api.pendientes(params) });

  protected readonly error = computed(() => {
    const e = this.indicadores.error() ?? this.pendientes.error();
    return e ? leerError(e).mensaje : null;
  });

  protected antiguedad(fecha: string): number {
    return diasDesde(fecha);
  }

  /** Exporta las vencidas mostradas (datos ya devueltos por la API). */
  protected exportar(): void {
    const filas = this.pendientes.value() ?? [];
    const cabecera = ['Codigo', 'Titulo', 'Categoria', 'Prioridad', 'Estado', 'Ciudadano', 'Area', 'Direccion', 'Fecha reporte', 'Dias'];
    const cuerpo = filas.map((i) => [
      i.id,
      i.titulo,
      etiqueta('categoria', i.categoria),
      etiqueta('prioridad', i.prioridad),
      etiqueta('estado', i.estado),
      i.ciudadanoNombre,
      i.areaResponsableNombre ?? 'Sin asignar',
      i.direccionReferencial ?? '',
      i.fechaReporte.slice(0, 16).replace('T', ' '),
      diasDesde(i.fechaReporte),
    ]);
    const csv = [cabecera, ...cuerpo]
      .map((fila) => fila.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(';'))
      .join('\r\n');
    const url = URL.createObjectURL(new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `incidencias-vencidas-${this.dias()}d.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
