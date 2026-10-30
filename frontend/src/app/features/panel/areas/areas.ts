import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AreasApi } from '../../../core/api/apis';
import { AreaResponse } from '../../../core/models/api.models';
import { aplicarErroresServidor, leerError } from '../../../core/services/api-error';
import { ConfirmService } from '../../../core/services/confirm.service';
import { ToastService } from '../../../core/services/toast.service';
import { Icon } from '../../../shared/components/icon';
import { EmptyState, FieldError, Loading, PageHeader } from '../../../shared/components/ui';

/** CRUD de /api/areas (AREA_GESTIONAR: ADMIN). Un area con incidencias o personal no se elimina (409). */
@Component({
  selector: 'ua-areas',
  imports: [ReactiveFormsModule, Icon, PageHeader, FieldError, EmptyState, Loading],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ua-page-header titulo="Áreas responsables" descripcion="Unidades municipales que atienden las incidencias derivadas.">
      <button type="button" class="btn btn-primary" (click)="nueva()"><ua-icon name="plus" />Nueva área</button>
    </ua-page-header>

    @if (editando() !== null) {
      <section class="card form-card">
        <div class="card-header"><h2>{{ editando() === 0 ? 'Nueva área' : 'Editar área' }}</h2></div>
        <form [formGroup]="form" (ngSubmit)="guardar()" novalidate>
          @if (error()) {
            <div class="alert alert-error"><ua-icon name="alert" />{{ error() }}</div>
          }
          <div class="field">
            <label for="a-nombre">Nombre</label>
            <input id="a-nombre" class="input" formControlName="nombre" maxlength="100" />
            <ua-field-error [control]="form.controls.nombre" requerido="Ingrese el nombre." />
          </div>
          <div class="field">
            <label for="a-desc">Descripción <span class="optional">opcional</span></label>
            <textarea id="a-desc" class="input" rows="3" formControlName="descripcion" maxlength="250"></textarea>
            <ua-field-error [control]="form.controls.descripcion" />
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" (click)="editando.set(null)">Cancelar</button>
            <button type="submit" class="btn btn-primary" [disabled]="guardando()">
              @if (guardando()) {
                <span class="spinner spinner-sm"></span>
              }
              Guardar
            </button>
          </div>
        </form>
      </section>
    }

    <section class="card card-flush">
      @if (lista.error()) {
        <div class="card-body"><div class="alert alert-error"><ua-icon name="alert" />{{ errorLista() }}</div></div>
      } @else if (lista.isLoading() && !lista.hasValue()) {
        <ua-loading />
      } @else if ((lista.value() ?? []).length === 0) {
        <ua-empty titulo="Sin áreas" mensaje="Registre la primera área responsable." icono="building" />
      } @else {
        <div class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                <th>Descripción</th>
                <th><span class="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (a of lista.value(); track a.id) {
                <tr>
                  <td class="code">{{ a.id }}</td>
                  <td class="cell-strong nowrap">{{ a.nombre }}</td>
                  <td class="cell-muted">{{ a.descripcion || '—' }}</td>
                  <td class="cell-actions">
                    <button type="button" class="btn btn-icon btn-ghost" (click)="editar(a)" aria-label="Editar"><ua-icon name="pencil" /></button>
                    <button type="button" class="btn btn-icon btn-ghost btn-ghost-danger" (click)="eliminar(a)" aria-label="Eliminar"><ua-icon name="trash" /></button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
})
export class Areas {
  private readonly api = inject(AreasApi);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);

  protected readonly lista = rxResource({ stream: () => this.api.listar() });
  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    descripcion: ['', Validators.maxLength(250)],
  });
  /** null: formulario cerrado · 0: nueva · id: edicion */
  protected readonly editando = signal<number | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly errorLista = computed(() => leerError(this.lista.error()).mensaje);

  protected nueva(): void {
    this.form.reset({ nombre: '', descripcion: '' });
    this.error.set(null);
    this.editando.set(0);
  }

  protected editar(a: AreaResponse): void {
    this.form.reset({ nombre: a.nombre, descripcion: a.descripcion ?? '' });
    this.error.set(null);
    this.editando.set(a.id);
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const datos = { nombre: v.nombre.trim(), descripcion: v.descripcion.trim() || null };
    const id = this.editando();
    this.guardando.set(true);
    this.error.set(null);
    (id ? this.api.actualizar(id, datos) : this.api.crear(datos)).subscribe({
      next: () => {
        this.toast.success(id ? 'Área actualizada.' : 'Área registrada.');
        this.guardando.set(false);
        this.editando.set(null);
        this.lista.reload();
      },
      error: (e) => {
        const err = leerError(e);
        aplicarErroresServidor(this.form, err.campos);
        this.error.set(err.mensaje);
        this.guardando.set(false);
      },
    });
  }

  protected async eliminar(a: AreaResponse): Promise<void> {
    const ok = await this.confirm.preguntar({
      titulo: 'Eliminar área',
      mensaje: `Se eliminará "${a.nombre}". No es posible si tiene incidencias o personal asignado.`,
      confirmar: 'Eliminar',
      peligro: true,
    });
    if (!ok) return;
    this.api.eliminar(a.id).subscribe({
      next: () => {
        this.toast.success('Área eliminada.');
        this.lista.reload();
      },
      error: (e) => this.toast.error(leerError(e).mensaje),
    });
  }
}
