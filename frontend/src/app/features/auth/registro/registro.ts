import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApi } from '../../../core/auth/auth.api';
import { aplicarErroresServidor, leerError } from '../../../core/services/api-error';
import { Icon } from '../../../shared/components/icon';
import { FieldError } from '../../../shared/components/ui';
import { PATRONES } from '../../../core/models/validacion';


function coinciden(grupo: AbstractControl): ValidationErrors | null {
  const p = grupo.get('password')?.value;
  const c = grupo.get('confirmacion');
  if (!c) return null;
  const errores = { ...(c.errors ?? {}) };
  if (c.value && p !== c.value) errores['coincidencia'] = true;
  else delete errores['coincidencia'];
  c.setErrors(Object.keys(errores).length ? errores : null);
  return null;
}

/**
 * POST /api/auth/register — crea perfil de ciudadano + cuenta con rol CIUDADANO.
 * El rol lo fija el servidor: el formulario no permite elegirlo.
 */
@Component({
  selector: 'ua-registro',
  imports: [ReactiveFormsModule, RouterLink, Icon, FieldError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="auth-card auth-card-wide">
      <h1>Crear cuenta de vecino</h1>
      <p class="muted">Regístrese para reportar incidencias y seguir su atención.</p>

      @if (error()) {
        <div class="alert alert-error" role="alert"><ua-icon name="alert" />{{ error() }}</div>
      }

      <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
        <fieldset class="form-section">
          <legend>Datos personales</legend>
          <div class="field">
            <label for="nombre">Nombres y apellidos</label>
            <input id="nombre" class="input" formControlName="nombre" autocomplete="name" maxlength="120" />
            <ua-field-error [control]="form.controls.nombre" requerido="Ingrese el nombre." />
          </div>
          <div class="grid-2">
            <div class="field">
              <label for="dni">DNI</label>
              <input id="dni" class="input" formControlName="dni" inputmode="numeric" maxlength="8" />
              <ua-field-error [control]="form.controls.dni" requerido="Ingrese el DNI." patron="El DNI debe tener 8 dígitos." />
            </div>
            <div class="field">
              <label for="telefono">Teléfono <span class="optional">opcional</span></label>
              <input id="telefono" class="input" formControlName="telefono" inputmode="tel" maxlength="9" autocomplete="tel" />
              <ua-field-error [control]="form.controls.telefono" patron="El teléfono debe tener 9 dígitos." />
            </div>
          </div>
          <div class="field">
            <label for="email">Correo electrónico <span class="optional">opcional</span></label>
            <input id="email" class="input" type="email" formControlName="email" autocomplete="email" maxlength="120" />
            <ua-field-error [control]="form.controls.email" />
          </div>
        </fieldset>

        <fieldset class="form-section">
          <legend>Datos de acceso</legend>
          <div class="field">
            <label for="username">Usuario</label>
            <input id="username" class="input" formControlName="username" autocomplete="username" maxlength="50" />
            <span class="hint">Entre 4 y 50 caracteres: letras, números, punto, guion o guion bajo.</span>
            <ua-field-error [control]="form.controls.username" requerido="Ingrese el usuario." patron="Solo letras, números, punto, guion y guion bajo." />
          </div>
          <div class="grid-2">
            <div class="field">
              <label for="password">Contraseña</label>
              <div class="input-group">
                <input id="password" class="input" [type]="ver() ? 'text' : 'password'" formControlName="password" autocomplete="new-password" maxlength="72" />
                <button type="button" class="input-addon" (click)="ver.set(!ver())" [attr.aria-label]="ver() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
                  <ua-icon [name]="ver() ? 'eye-off' : 'eye'" />
                </button>
              </div>
              <span class="hint">Mínimo 8 caracteres, con al menos una letra y un número.</span>
              <ua-field-error [control]="form.controls.password" requerido="Ingrese la contraseña." patron="Mínimo 8 caracteres, con al menos una letra y un número." />
            </div>
            <div class="field">
              <label for="confirmacion">Confirmar contraseña</label>
              <input id="confirmacion" class="input" [type]="ver() ? 'text' : 'password'" formControlName="confirmacion" autocomplete="new-password" />
              <ua-field-error [control]="form.controls.confirmacion" requerido="Confirme la contraseña." />
            </div>
          </div>
        </fieldset>

        <button type="submit" class="btn btn-primary btn-block" [disabled]="cargando()">
          @if (cargando()) {
            <span class="spinner spinner-sm"></span> Creando cuenta…
          } @else {
            Crear cuenta
          }
        </button>
      </form>

      <p class="auth-switch">¿Ya tiene cuenta? <a routerLink="/login">Inicie sesión</a></p>
    </div>
  `,
})
export class Registro {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly auth = inject(AuthApi);
  private readonly router = inject(Router);

  protected readonly form = this.fb.group(
    {
      nombre: ['', [Validators.required, Validators.maxLength(120)]],
      dni: ['', [Validators.required, Validators.pattern(PATRONES.dni)]],
      telefono: ['', Validators.pattern(PATRONES.telefono)],
      email: ['', [Validators.email, Validators.maxLength(120)]],
      username: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50), Validators.pattern(PATRONES.username)]],
      password: ['', [Validators.required, Validators.pattern(PATRONES.password)]],
      confirmacion: ['', Validators.required],
    },
    { validators: coinciden },
  );
  protected readonly ver = signal(false);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { confirmacion: _c, ...v } = this.form.getRawValue();
    this.cargando.set(true);
    this.error.set(null);
    this.auth
      .registrar({
        username: v.username.trim(),
        password: v.password,
        nombre: v.nombre.trim(),
        dni: v.dni.trim(),
        email: v.email.trim() || null,
        telefono: v.telefono.trim() || null,
      })
      .subscribe({
        next: () => this.router.navigate(['/login'], { queryParams: { registrado: 1 } }),
        error: (e) => {
          const err = leerError(e);
          aplicarErroresServidor(this.form, err.campos);
          this.error.set(err.mensaje);
          this.cargando.set(false);
        },
      });
  }
}
