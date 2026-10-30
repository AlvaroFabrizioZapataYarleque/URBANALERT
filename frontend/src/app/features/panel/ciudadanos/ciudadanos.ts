import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { CiudadanosApi } from '../../../core/api/apis';
import { CiudadanoResponse } from '../../../core/models/api.models';
import { leerError } from '../../../core/services/api-error';
import { ToastService } from '../../../core/services/toast.service';
import { Icon } from '../../../shared/components/icon';
import { EmptyState, Loading, PageHeader, Paginator } from '../../../shared/components/ui';
import { createTableState } from '../../../shared/utils/table-state';
import { CiudadanoForm } from './ciudadano-form';

/** GET /api/ciudadanos (CIUDADANO_GESTIONAR: ADMIN y OPERADOR). */
@Component({
  selector: 'ua-ciudadanos',
  imports: [Icon, PageHeader, Paginator, EmptyState, Loading, CiudadanoForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ua-page-header titulo="Ciudadanos" descripcion="Vecinos registrados, con o sin cuenta en el portal.">
      <button type="button" class="btn btn-primary" (click)="formAbierto.set(true)"><ua-icon name="user-plus" />Nuevo ciudadano</button>
    </ua-page-header>

    @if (formAbierto()) {
      <section class="card form-card">
        <div class="card-header">
          <div>
            <h2>Registrar ciudadano</h2>
            <p class="muted small">Registro sin cuenta de acceso, para atención presencial o telefónica. Con este DNI el vecino ya no podrá crear su propia cuenta en el portal.</p>
          </div>
        </div>
        <ua-ciudadano-form (creado)="creado($event)" (cancelado)="formAbierto.set(false)" />
      </section>
    }

    <section class="card card-flush">
      <div class="toolbar">
        <div class="search">
          <ua-icon name="search" />
          <input class="input" type="search" placeholder="Buscar por nombre, DNI, correo o teléfono" [value]="tabla.texto()" (input)="tabla.buscar($any($event.target).value)" />
        </div>
        <span class="muted small push-right">{{ tabla.total() }} ciudadanos</span>
      </div>
      @if (lista.error()) {
        <div class="card-body"><div class="alert alert-error"><ua-icon name="alert" />{{ errorMsg() }}</div></div>
      } @else if (lista.isLoading() && !lista.hasValue()) {
        <ua-loading />
      } @else if (tabla.total() === 0) {
        <ua-empty titulo="Sin resultados" mensaje="No se encontraron ciudadanos." icono="users" />
      } @else {
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                <th>DNI</th>
                <th>Correo</th>
                <th>Teléfono</th>
                <th><span class="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (c of tabla.visibles(); track c.id) {
                <tr>
                  <td class="code">{{ c.id }}</td>
                  <td class="cell-strong">{{ c.nombre }}</td>
                  <td>{{ c.dni }}</td>
                  <td class="cell-muted">{{ c.email || '—' }}</td>
                  <td>{{ c.telefono || '—' }}</td>
                  <td class="cell-actions">
                    <button type="button" class="btn btn-sm btn-secondary" (click)="reportar(c)">Registrar incidencia</button>
                  </td>
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
export class Ciudadanos {
  private readonly api = inject(CiudadanosApi);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly formAbierto = signal(false);
  protected readonly lista = rxResource({ stream: () => this.api.listar() });
  private readonly filas = computed<CiudadanoResponse[]>(() => this.lista.value() ?? []);
  protected readonly tabla = createTableState(this.filas, {
    buscarEn: (c) => [c.nombre, c.dni, c.email, c.telefono],
    ordenarPor: { nombre: (c) => c.nombre.toLowerCase() },
    ordenInicial: 'nombre',
    dirInicial: 'asc',
  });
  protected readonly errorMsg = computed(() => leerError(this.lista.error()).mensaje);

  protected creado(c: CiudadanoResponse): void {
    this.toast.success(`Ciudadano ${c.nombre} registrado.`);
    this.formAbierto.set(false);
    this.lista.reload();
  }

  protected reportar(c: CiudadanoResponse): void {
    this.router.navigate(['/panel/registro'], { queryParams: { ciudadano: c.id } });
  }
}
