import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthApi } from '../../../core/auth/auth.api';
import { SessionService } from '../../../core/auth/session.service';
import { leerError } from '../../../core/services/api-error';
import { Icon } from '../../../shared/components/icon';
import { FieldError } from '../../../shared/components/ui';

/** POST /api/auth/login — el backend autentica por username (no por correo). */
@Component({
  selector: 'ua-login',
  imports: [ReactiveFormsModule, RouterLink, Icon, FieldError],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="auth-card">
      <h1>Iniciar sesión</h1>
      <p class="muted">Ingrese con su usuario y contraseña.</p>

      @if (aviso()) {
        <div class="alert alert-info"><ua-icon name="info" />{{ aviso() }}</div>
      }
      @if (error()) {
        <div class="alert alert-error" role="alert"><ua-icon name="alert" />{{ error() }}</div>
      }

      <form [formGroup]="form" (ngSubmit)="enviar()" novalidate>
        <div class="field">
          <label for="username">Usuario</label>
          <input id="username" class="input" formControlName="username" autocomplete="username" autofocus />
          <ua-field-error [control]="form.controls.username" requerido="Ingrese el usuario." />
        </div>

        <div class="field">
          <label for="password">Contraseña</label>
          <div class="input-group">
            <input
              id="password"
              class="input"
              [type]="verPassword() ? 'text' : 'password'"
              formControlName="password"
              autocomplete="current-password"
            />
            <button
              type="button"
              class="input-addon"
              (click)="verPassword.set(!verPassword())"
              [attr.aria-label]="verPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'"
            >
              <ua-icon [name]="verPassword() ? 'eye-off' : 'eye'" />
            </button>
          </div>
          <ua-field-error [control]="form.controls.password" requerido="Ingrese la contraseña." />
        </div>

        <button type="submit" class="btn btn-primary btn-block" [disabled]="cargando()">
          @if (cargando()) {
            <span class="spinner spinner-sm"></span> Ingresando…
          } @else {
            Ingresar
          }
        </button>
      </form>

      <p class="auth-switch">¿Es vecino y aún no tiene cuenta? <a routerLink="/registro">Regístrese</a></p>
    </div>
  `,
})
export class Login {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly auth = inject(AuthApi);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });
  protected readonly verPassword = signal(false);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly aviso = signal<string | null>(this.avisoInicial());

  protected enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.cargando.set(true);
    this.error.set(null);
    this.aviso.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const volver = this.route.snapshot.queryParamMap.get('volver');
        // Solo rutas internas del rol correspondiente; si no, el inicio del rol.
        const destino = volver && volver.startsWith(this.session.inicio()) ? volver : this.session.inicio();
        this.router.navigateByUrl(destino);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(leerError(e).mensaje);
      },
    });
  }

  private avisoInicial(): string | null {
    const q = this.route.snapshot.queryParamMap;
    if (q.get('motivo') === 'expirada') return 'Su sesión expiró. Inicie sesión nuevamente.';
    if (q.get('registrado')) return 'Cuenta creada. Ya puede iniciar sesión.';
    return null;
  }
}
