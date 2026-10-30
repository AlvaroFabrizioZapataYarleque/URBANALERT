import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CiudadanosApi } from '../../../core/api/apis';
import { SessionService } from '../../../core/auth/session.service';
import { CiudadanoResponse } from '../../../core/models/api.models';
import { aplicarErroresServidor, leerError } from '../../../core/services/api-error';
import { ToastService } from '../../../core/services/toast.service';
import { Icon } from '../../../shared/components/icon';
import { FieldError, Loading, PageHeader } from '../../../shared/components/ui';
import { PATRONES } from '../../../core/models/validacion';

/** GET/PUT /api/ciudadanos/me (PerfilRequest: nombre, email, telefono; el DNI no se modifica). */
@Component({
  selector: 'ua-perfil',
  imports: [ReactiveFormsModule, Icon, PageHeader, FieldError, Loading],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ua-page-header titulo="Mi perfil" descripcion="Datos de contacto que verá el municipio al atender sus reportes." />

    @if (cargando()) {
      <ua-loading />
    } @else if (perfil(); as p) {
      <form class="card form-card" [formGroup]="form" (ngSubmit)="guardar()" novalidate>
        @if (error()) {
          <div class="alert alert-error"><ua-icon name="alert" />{{ error() }}</div>
        }
        <fieldset class="form-section">
          <legend>Cuenta</legend>
          <div class="grid-2">
            <div class="field">
              <span class="label">Usuario</span>
              <input class="input" [value]="session.username()" disabled />
            </div>
            <div class="field">
              <span class="label">DNI</span>
              <input class="input" [value]="p.dni" disabled />
              <span class="hint">El DNI no puede modificarse.</span>
            </div>
          </div>
        </fieldset>
        <fieldset class="form-section">
          <legend>Datos de contacto</legend>
          <div class="field">
            <label for="nombre">Nombres y apellidos</label>
            <input id="nombre" class="input" formControlName="nombre" maxlength="120" />
            <ua-field-error [control]="form.controls.nombre" requerido="Ingrese el nombre." />
          </div>
          <div class="grid-2">
            <div class="field">
              <label for="email">Correo electrónico <span class="optional">opcional</span></label>
              <input id="email" class="input" type="email" formControlName="email" maxlength="120" />
              <ua-field-error [control]="form.controls.email" />
            </div>
            <div class="field">
              <label for="telefono">Teléfono <span class="optional">opcional</span></label>
              <input id="telefono" class="input" formControlName="telefono" maxlength="9" inputmode="tel" />
              <ua-field-error [control]="form.controls.telefono" patron="El teléfono debe tener 9 dígitos." />
            </div>
          </div>
        </fieldset>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" [disabled]="guardando() || form.pristine">
            @if (guardando()) {
              <span class="spinner spinner-sm"></span> Guardando…
            } @else {
              Guardar cambios
            }
          </button>
        </div>
      </form>
    } @else if (error()) {
      <div class="alert alert-error"><ua-icon name="alert" />{{ error() }}</div>
    }
  `,
})
export class Perfil implements OnInit {
  private readonly api = inject(CiudadanosApi);
  private readonly toast = inject(ToastService);
  protected readonly session = inject(SessionService);

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    telefono: ['', Validators.pattern(PATRONES.telefono)],
  });
  protected readonly perfil = signal<CiudadanoResponse | null>(null);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.api.miPerfil().subscribe({
      next: (p) => this.cargar(p),
      error: (e) => {
        this.error.set(leerError(e).mensaje);
        this.cargando.set(false);
      },
    });
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set(null);
    this.api
      .actualizarMiPerfil({ nombre: v.nombre.trim(), email: v.email.trim() || null, telefono: v.telefono.trim() || null })
      .subscribe({
        next: (p) => {
          this.cargar(p);
          this.guardando.set(false);
          this.toast.success('Perfil actualizado.');
        },
        error: (e) => {
          const err = leerError(e);
          aplicarErroresServidor(this.form, err.campos);
          this.error.set(err.mensaje);
          this.guardando.set(false);
        },
      });
  }

  private cargar(p: CiudadanoResponse): void {
    this.perfil.set(p);
    this.form.reset({ nombre: p.nombre, email: p.email ?? '', telefono: p.telefono ?? '' });
    this.cargando.set(false);
  }
}
