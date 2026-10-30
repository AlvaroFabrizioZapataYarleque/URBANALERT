import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CiudadanosApi } from '../../../core/api/apis';
import { CiudadanoResponse } from '../../../core/models/api.models';
import { PATRONES } from '../../../core/models/validacion';
import { aplicarErroresServidor, leerError } from '../../../core/services/api-error';
import { Icon } from '../../../shared/components/icon';
import { FieldError } from '../../../shared/components/ui';

/** POST /api/ciudadanos — alta de un vecino sin cuenta (atencion presencial o telefonica). */
@Component({
  selector: 'ua-ciudadano-form',
  imports: [ReactiveFormsModule, Icon, FieldError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" (ngSubmit)="guardar()" novalidate>
      @if (error()) {
        <div class="alert alert-error"><ua-icon name="alert" />{{ error() }}</div>
      }
      <div class="field">
        <label for="c-nombre">Nombres y apellidos</label>
        <input id="c-nombre" class="input" formControlName="nombre" maxlength="120" />
        <ua-field-error [control]="form.controls.nombre" requerido="Ingrese el nombre." />
      </div>
      <div class="grid-2">
        <div class="field">
          <label for="c-dni">DNI</label>
          <input id="c-dni" class="input" formControlName="dni" maxlength="8" inputmode="numeric" />
          <ua-field-error [control]="form.controls.dni" requerido="Ingrese el DNI." patron="El DNI debe tener 8 dígitos." />
        </div>
        <div class="field">
          <label for="c-tel">Teléfono <span class="optional">opcional</span></label>
          <input id="c-tel" class="input" formControlName="telefono" maxlength="9" inputmode="tel" />
          <ua-field-error [control]="form.controls.telefono" patron="El teléfono debe tener 9 dígitos." />
        </div>
      </div>
      <div class="field">
        <label for="c-email">Correo electrónico <span class="optional">opcional</span></label>
        <input id="c-email" class="input" type="email" formControlName="email" maxlength="120" />
        <ua-field-error [control]="form.controls.email" />
      </div>
      <div class="form-actions">
        <button type="button" class="btn btn-secondary" (click)="cancelado.emit()">Cancelar</button>
        <button type="submit" class="btn btn-primary" [disabled]="guardando()">
          @if (guardando()) {
            <span class="spinner spinner-sm"></span>
          }
          Registrar ciudadano
        </button>
      </div>
    </form>
  `,
})
export class CiudadanoForm {
  private readonly api = inject(CiudadanosApi);
  readonly creado = output<CiudadanoResponse>();
  readonly cancelado = output<void>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    dni: ['', [Validators.required, Validators.pattern(PATRONES.dni)]],
    telefono: ['', Validators.pattern(PATRONES.telefono)],
    email: ['', [Validators.email, Validators.maxLength(120)]],
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set(null);
    this.api
      .registrar({ nombre: v.nombre.trim(), dni: v.dni.trim(), telefono: v.telefono.trim() || null, email: v.email.trim() || null })
      .subscribe({
        next: (c) => {
          this.guardando.set(false);
          this.form.reset();
          this.creado.emit(c);
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
