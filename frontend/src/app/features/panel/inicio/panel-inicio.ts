import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { of } from 'rxjs';
import { AreasApi, IncidenciasApi, ReportesApi } from '../../../core/api/apis';
import { SessionService } from '../../../core/auth/session.service';
import { estaPendiente } from '../../../core/models/catalogos';
import { leerError } from '../../../core/services/api-error';
import { EstadoBadge, PrioridadBadge } from '../../../shared/components/badges';
import { Icon } from '../../../shared/components/icon';
import { BarChart, BarDatum, EmptyState, Loading, PageHeader, StatCard } from '../../../shared/components/ui';
import { LabelPipe } from '../../../shared/pipes/label.pipe';
import {
  RANGO_PRIORIDAD,
  barrasCategoria,
  barrasEstado,
  barrasPrioridad,
  contarPor,
  diasDesde,
} from '../../../shared/utils/agregados';

/**
 * Dashboard del panel.
 *  - ADMIN: indicadores reales de GET /api/reportes/indicadores (REPORTE_VER) y cortes por
 *    prioridad/area calculados sobre GET /api/incidencias filtrado por periodo y area.
 *  - OPERADOR: no tiene REPORTE_VER; las cifras se calculan sobre las incidencias de su area.
 */
@Component({
  selector: 'ua-panel-inicio',
  imports: [RouterLink, DatePipe, DecimalPipe, Icon, PageHeader, StatCard, BarChart, Loading, EmptyState, EstadoBadge, PrioridadBadge, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './panel-inicio.html',
})
export class PanelInicio {
  protected readonly String = String;
  private readonly incidenciasApi = inject(IncidenciasApi);
  private readonly reportesApi = inject(ReportesApi);
  private readonly areasApi = inject(AreasApi);
  protected readonly session = inject(SessionService);
  protected readonly esAdmin = computed(() => this.session.tieneRol('ADMIN'));

  // Filtros
  protected readonly dias = signal(7);
  protected readonly desde = signal('');
  protected readonly hasta = signal('');
  protected readonly areaId = signal('');

  protected readonly indicadores = rxResource({
    params: () => (this.esAdmin() ? this.dias() : null),
    stream: ({ params }) => (params ? this.reportesApi.indicadores(params) : of(null)),
  });

  protected readonly areas = rxResource({ stream: () => this.areasApi.listar() });

  protected readonly lista = rxResource({
    params: () => ({ desde: this.desde() || null, hasta: this.hasta() || null, areaId: this.areaId() ? +this.areaId() : null }),
    stream: ({ params }) => this.incidenciasApi.listar(params),
  });

  protected readonly error = computed(() => {
    const e = this.indicadores.error() ?? this.lista.error();
    return e ? leerError(e).mensaje : null;
  });

  private readonly filas = computed(() => this.lista.value() ?? []);

  /** Tarjetas: del endpoint de indicadores (ADMIN) o del listado del area (OPERADOR). */
  protected readonly tarjetas = computed(() => {
    const ind = this.indicadores.value();
    const porEstado = ind ? ind.porEstado : contarPor(this.filas(), (i) => i.estado);
    const total = ind ? ind.total : this.filas().length;
    return {
      total,
      registradas: porEstado.REGISTRADA ?? 0,
      enProceso: porEstado.EN_PROCESO ?? 0,
      resueltas: (porEstado.RESUELTA ?? 0) + (porEstado.CERRADA ?? 0),
    };
  });

  protected readonly barrasEstado = computed<BarDatum[]>(() => {
    const ind = this.indicadores.value();
    return barrasEstado(ind ? ind.porEstado : contarPor(this.filas(), (i) => i.estado));
  });

  protected readonly barrasCategoria = computed<BarDatum[]>(() => {
    const ind = this.indicadores.value();
    return barrasCategoria(ind ? ind.porCategoria : contarPor(this.filas(), (i) => i.categoria));
  });

  protected readonly barrasTiempo = computed<BarDatum[]>(() => {
    const t = this.indicadores.value()?.tiempoPromedioResolucionHorasPorCategoria ?? {};
    return barrasCategoria(t).filter((d) => d.valor > 0);
  });

  protected readonly barrasPrioridad = computed(() => barrasPrioridad(contarPor(this.filas(), (i) => i.prioridad)));

  protected readonly barrasArea = computed<BarDatum[]>(() =>
    Object.entries(contarPor(this.filas(), (i) => i.areaResponsableNombre ?? 'Sin asignar'))
      .map(([etiqueta, valor]) => ({ etiqueta, valor: valor ?? 0, tono: etiqueta === 'Sin asignar' ? 'tone-slate' : 'tone-blue' }))
      .sort((a, b) => b.valor - a.valor),
  );

  /** Pendientes del alcance ordenadas por prioridad y antiguedad. */
  protected readonly porAtender = computed(() =>
    this.filas()
      .filter((i) => estaPendiente(i.estado))
      .sort((a, b) => RANGO_PRIORIDAD[b.prioridad] - RANGO_PRIORIDAD[a.prioridad] || a.fechaReporte.localeCompare(b.fechaReporte))
      .slice(0, 6),
  );

  protected readonly sinArea = computed(() => this.filas().filter((i) => !i.areaResponsableId && i.estado !== 'CERRADA').length);

  protected readonly hayFiltroPeriodo = computed(() => !!(this.desde() || this.hasta() || this.areaId()));

  protected dias_(fecha: string): number {
    return diasDesde(fecha);
  }

  protected limpiar(): void {
    this.desde.set('');
    this.hasta.set('');
    this.areaId.set('');
  }

  protected recargar(): void {
    this.indicadores.reload();
    this.lista.reload();
  }
}
