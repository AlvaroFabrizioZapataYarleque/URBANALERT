import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { IncidenciasApi } from '../../../core/api/apis';
import { aplicarErroresServidor, leerError } from '../../../core/services/api-error';
import { ToastService } from '../../../core/services/toast.service';
import { Icon } from '../../../shared/components/icon';
import { IncidenciaCampos, crearIncidenciaForm, valorIncidencia } from '../../../shared/components/incidencia-campos';
import { Loading, PageHeader } from '../../../shared/components/ui';

/**
 * Registrar (POST /api/incidencias) o editar (PUT /api/incidencias/{id}) un reporte propio.
 * El backend solo permite editar mientras la incidencia esta REGISTRADA.
 */
@Component({
  selector: 'ua-portal-incidencia-form',
  imports: [ReactiveFormsModule, RouterLink, Icon, PageHeader, Loading, IncidenciaCampos],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="back-link" [routerLink]="id() ? ['/portal/incidencias', id()] : '/portal/incidencias'"><ua-icon name="arrow-left" [size]="16" />Volver</a>
    <ua-page-header
      [titulo]="id() ? 'Editar incidencia #' + id() : 'Reportar incidencia'"
      [descripcion]="id() ? 'Puede modificar el reporte mientras no haya sido atendido.' : 'Complete los datos para que el área correspondiente pueda atender el problema.'"
    />

    @if (cargando()) {
      <ua-loading />
    } @else if (bloqueo()) {
      <div class="alert alert-info"><ua-icon name="info" />{{ bloqueo() }}</div>
    } @else {
      <form class="card form-card" [formGroup]="form" (ngSubmit)="guardar()" novalidate>
        @if (error()) {
          <div class="alert alert-error" role="alert"><ua-icon name="alert" />{{ error() }}</div>
        }
        <ua-incidencia-campos
          [form]="form"
          etiquetaPrioridad="Urgencia percibida"
          ayudaPrioridad="Indique qué tan urgente le parece. El municipio puede reclasificarla."
        />
        <div class="form-actions">
          <a class="btn btn-secondary" [routerLink]="id() ? ['/portal/incidencias', id()] : '/portal'">Cancelar</a>
          <button type="submit" class="btn btn-primary" [disabled]="guardando()">
            @if (guardando()) {
              <span class="spinner spinner-sm"></span> Guardando…
            } @else {
              {{ id() ? 'Guardar cambios' : 'Enviar reporte' }}
            }
          </button>
        </div>
      </form>
    }
  `,
})
export class PortalIncidenciaForm implements OnInit {
  /** Parametro de ruta :id (solo en edicion). */
  readonly id = input<string>();

  private readonly api = inject(IncidenciasApi);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  protected readonly form = crearIncidenciaForm(inject(NonNullableFormBuilder));

  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly bloqueo = signal<string | null>(null);

  ngOnInit(): void {
    const id = this.id();
    if (!id) return;
    this.cargando.set(true);
    this.api.obtener(+id).subscribe({
      next: (i) => {
        if (i.estado !== 'REGISTRADA') {
          this.bloqueo.set('Esta incidencia ya está siendo atendida y no puede modificarse.');
        }
        this.form.patchValue({
          titulo: i.titulo,
          descripcion: i.descripcion,
          categoria: i.categoria,
          prioridad: i.prioridad,
          direccionReferencial: i.direccionReferencial ?? '',
        });
        this.cargando.set(false);
      },
      error: (e) => {
        this.bloqueo.set(leerError(e).mensaje);
        this.cargando.set(false);
      },
    });
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const id = this.id();
    const datos = valorIncidencia(this.form);
    this.guardando.set(true);
    this.error.set(null);
    const peticion = id ? this.api.actualizar(+id, datos) : this.api.registrar(datos);
    peticion.subscribe({
      next: (i) => {
        this.toast.success(id ? 'Incidencia actualizada.' : `Incidencia #${i.id} registrada.`);
        this.router.navigate(['/portal/incidencias', i.id]);
      },
      error: (e) => {
        const err = leerError(e);
        aplicarErroresServidor(this.form, err.campos);
        this.error.set(err.mensaje);
        this.guardando.set(false);
      },
    });
  }
}
