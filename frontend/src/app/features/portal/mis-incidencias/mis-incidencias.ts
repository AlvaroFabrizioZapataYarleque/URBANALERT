import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { IncidenciasApi } from '../../../core/api/apis';
import { CategoriaIncidencia, EstadoIncidencia, IncidenciaResponse } from '../../../core/models/api.models';
import { CATEGORIAS, ESTADOS } from '../../../core/models/catalogos';
import { leerError } from '../../../core/services/api-error';
import { EstadoBadge, PrioridadBadge } from '../../../shared/components/badges';
import { Icon } from '../../../shared/components/icon';
import { EmptyState, Loading, PageHeader, Paginator } from '../../../shared/components/ui';
import { LabelPipe } from '../../../shared/pipes/label.pipe';
import { createTableState } from '../../../shared/utils/table-state';

@Component({
  selector: 'ua-mis-incidencias',
  imports: [RouterLink, DatePipe, Icon, PageHeader, Paginator, EmptyState, Loading, EstadoBadge, PrioridadBadge, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ua-page-header titulo="Mis incidencias" descripcion="Reportes que usted ha registrado y su estado de atención.">
      <a class="btn btn-primary" routerLink="/portal/reportar"><ua-icon name="plus" />Reportar incidencia</a>
    </ua-page-header>

    <section class="card card-flush">
      <div class="toolbar">
        <div class="search">
          <ua-icon name="search" />
          <input class="input" type="search" placeholder="Buscar por código, título o dirección" [value]="tabla.texto()" (input)="tabla.buscar($any($event.target).value)" />
        </div>
        <select class="input" [value]="estado()" (change)="estado.set($any($event.target).value)" aria-label="Estado">
          <option value="">Todos los estados</option>
          @for (e of estados; track e.value) {
            <option [value]="e.value" [selected]="e.value === estado()">{{ e.label }}</option>
          }
        </select>
        <select class="input" [value]="categoria()" (change)="categoria.set($any($event.target).value)" aria-label="Categoría">
          <option value="">Todas las categorías</option>
          @for (c of categorias; track c.value) {
            <option [value]="c.value" [selected]="c.value === categoria()">{{ c.label }}</option>
          }
        </select>
      </div>

      @if (lista.error()) {
        <div class="card-body"><div class="alert alert-error"><ua-icon name="alert" />{{ errorMsg() }}</div></div>
      } @else if (lista.isLoading()) {
        <ua-loading />
      } @else if (tabla.total() === 0) {
        <ua-empty titulo="No hay incidencias" mensaje="No se encontraron reportes con los filtros seleccionados." />
      } @else {
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Código</th>
                <th>Incidencia</th>
                <th>Categoría</th>
                <th>Ubicación</th>
                <th>Fecha de registro</th>
                <th>Prioridad</th>
                <th>Estado</th>
                <th><span class="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (i of tabla.visibles(); track i.id) {
                <tr>
                  <td class="code">#{{ i.id }}</td>
                  <td class="cell-strong">{{ i.titulo }}</td>
                  <td>{{ i.categoria | label: 'categoria' }}</td>
                  <td class="cell-muted">{{ i.direccionReferencial || '—' }}</td>
                  <td class="nowrap">{{ i.fechaReporte | date: 'dd/MM/yyyy HH:mm' }}</td>
                  <td><ua-prioridad-badge [prioridad]="i.prioridad" /></td>
                  <td><ua-estado-badge [estado]="i.estado" /></td>
                  <td class="cell-actions"><a class="btn btn-sm btn-secondary" [routerLink]="['/portal/incidencias', i.id]">Ver detalle</a></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <ua-paginator [total]="tabla.total()" [pagina]="tabla.pagina()" [tamano]="tabla.tamano()" (paginaChange)="tabla.irA($event)" (tamanoChange)="tabla.cambiarTamano($event)" />
      }
    </section>
  `,
})
export class MisIncidencias {
  private readonly api = inject(IncidenciasApi);
  protected readonly estados = ESTADOS;
  protected readonly categorias = CATEGORIAS;

  protected readonly estado = signal<EstadoIncidencia | ''>('');
  protected readonly categoria = signal<CategoriaIncidencia | ''>('');

  // Estado y categoria se filtran en el servidor (IncidenciaFiltro); texto y paginas en el cliente.
  protected readonly lista = rxResource({
    params: () => ({ estado: this.estado() || null, categoria: this.categoria() || null }),
    stream: ({ params }) => this.api.listar(params),
  });
  private readonly filas = computed<IncidenciaResponse[]>(() => this.lista.value() ?? []);
  protected readonly tabla = createTableState(this.filas, {
    buscarEn: (i) => [i.id, `#${i.id}`, i.titulo, i.direccionReferencial],
    ordenarPor: { fecha: (i) => i.fechaReporte },
    ordenInicial: 'fecha',
  });
  protected readonly errorMsg = computed(() => leerError(this.lista.error()).mensaje);
}
