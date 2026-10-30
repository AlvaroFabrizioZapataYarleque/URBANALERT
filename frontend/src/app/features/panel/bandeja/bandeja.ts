import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { AreasApi, IncidenciasApi } from '../../../core/api/apis';
import { SessionService } from '../../../core/auth/session.service';
import { CategoriaIncidencia, EstadoIncidencia, IncidenciaFiltro, PrioridadIncidencia } from '../../../core/models/api.models';
import { CATEGORIAS, ESTADOS, PRIORIDADES } from '../../../core/models/catalogos';
import { leerError } from '../../../core/services/api-error';
import { EstadoBadge, PrioridadBadge } from '../../../shared/components/badges';
import { Icon } from '../../../shared/components/icon';
import { EmptyState, Loading, PageHeader, Paginator } from '../../../shared/components/ui';
import { LabelPipe } from '../../../shared/pipes/label.pipe';
import { RANGO_ESTADO, RANGO_PRIORIDAD } from '../../../shared/utils/agregados';
import { createTableState } from '../../../shared/utils/table-state';

/**
 * Bandeja de incidencias: GET /api/incidencias con filtros del servidor (estado, categoria,
 * prioridad, areaId, desde, hasta). Busqueda, orden y paginacion en el cliente.
 * El OPERADOR solo recibe las de su area (el servidor fija el alcance).
 */
@Component({
  selector: 'ua-bandeja',
  imports: [RouterLink, DatePipe, Icon, PageHeader, Paginator, EmptyState, Loading, EstadoBadge, PrioridadBadge, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './bandeja.html',
})
export class Bandeja {
  protected readonly String = String;
  private readonly api = inject(IncidenciasApi);
  private readonly areasApi = inject(AreasApi);
  private readonly router = inject(Router);
  protected readonly session = inject(SessionService);
  protected readonly esAdmin = computed(() => this.session.tieneRol('ADMIN'));

  protected readonly estados = ESTADOS;
  protected readonly categorias = CATEGORIAS;
  protected readonly prioridades = PRIORIDADES;

  protected readonly estado = signal<EstadoIncidencia | ''>('');
  protected readonly categoria = signal<CategoriaIncidencia | ''>('');
  protected readonly prioridad = signal<PrioridadIncidencia | ''>('');
  protected readonly areaId = signal('');
  protected readonly desde = signal('');
  protected readonly hasta = signal('');
  protected readonly soloSinArea = signal(false);

  protected readonly areas = rxResource({ stream: () => this.areasApi.listar() });

  protected readonly lista = rxResource({
    params: (): IncidenciaFiltro => ({
      estado: this.estado() || null,
      categoria: this.categoria() || null,
      prioridad: this.prioridad() || null,
      areaId: this.areaId() ? +this.areaId() : null,
      desde: this.desde() || null,
      hasta: this.hasta() || null,
    }),
    stream: ({ params }) => this.api.listar(params),
  });

  private readonly filas = computed(() => {
    const lista = this.lista.value() ?? [];
    return this.soloSinArea() ? lista.filter((i) => !i.areaResponsableId) : lista;
  });

  protected readonly tabla = createTableState(this.filas, {
    buscarEn: (i) => [i.id, `#${i.id}`, i.titulo, i.descripcion, i.ciudadanoNombre, i.direccionReferencial, i.areaResponsableNombre],
    ordenarPor: {
      id: (i) => i.id,
      fecha: (i) => i.fechaReporte,
      ciudadano: (i) => i.ciudadanoNombre.toLowerCase(),
      categoria: (i) => i.categoria,
      prioridad: (i) => RANGO_PRIORIDAD[i.prioridad],
      estado: (i) => RANGO_ESTADO[i.estado],
      area: (i) => (i.areaResponsableNombre ?? '').toLowerCase(),
    },
    ordenInicial: 'fecha',
    tamano: 10,
  });

  protected readonly errorMsg = computed(() => leerError(this.lista.error()).mensaje);
  protected readonly filtrosActivos = computed(
    () =>
      [this.estado(), this.categoria(), this.prioridad(), this.areaId(), this.desde(), this.hasta()].filter(Boolean).length +
      (this.soloSinArea() ? 1 : 0),
  );

  protected limpiar(): void {
    this.estado.set('');
    this.categoria.set('');
    this.prioridad.set('');
    this.areaId.set('');
    this.desde.set('');
    this.hasta.set('');
    this.soloSinArea.set(false);
    this.tabla.buscar('');
  }

  protected abrir(id: number): void {
    this.router.navigate(['/panel/incidencias', id]);
  }

  protected icono(clave: string) {
    if (this.tabla.orden() !== clave) return 'sort' as const;
    return this.tabla.dir() === 'asc' ? ('chevron-up' as const) : ('chevron-down' as const);
  }
}
