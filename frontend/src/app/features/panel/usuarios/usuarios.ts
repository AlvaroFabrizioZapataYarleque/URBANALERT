import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AreasApi, UsuariosApi } from '../../../core/api/apis';
import { SessionService } from '../../../core/auth/session.service';
import { NombreRol, UsuarioResponse } from '../../../core/models/api.models';
import { PATRONES } from '../../../core/models/validacion';
import { aplicarErroresServidor, leerError } from '../../../core/services/api-error';
import { ConfirmService } from '../../../core/services/confirm.service';
import { ToastService } from '../../../core/services/toast.service';
import { Icon } from '../../../shared/components/icon';
import { EmptyState, FieldError, Loading, PageHeader, Paginator } from '../../../shared/components/ui';
import { LabelPipe } from '../../../shared/pipes/label.pipe';
import { createTableState } from '../../../shared/utils/table-state';

/**
 * /api/usuarios (solo ADMIN): listado de cuentas, alta de personal (ADMIN u OPERADOR con area)
 * y habilitar/deshabilitar. Los ciudadanos se registran solos en /registro.
 */
@Component({
  selector: 'ua-usuarios',
  imports: [ReactiveFormsModule, DatePipe, Icon, PageHeader, FieldError, EmptyState, Loading, Paginator, LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './usuarios.html',
})
export class Usuarios {
  private readonly api = inject(UsuariosApi);
  private readonly areasApi = inject(AreasApi);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  protected readonly session = inject(SessionService);

  protected readonly lista = rxResource({ stream: () => this.api.listar() });
  protected readonly areas = rxResource({ stream: () => this.areasApi.listar() });

  protected readonly rolFiltro = signal<NombreRol | ''>('');
  private readonly filas = computed<UsuarioResponse[]>(() => {
    const rol = this.rolFiltro();
    const lista = this.lista.value() ?? [];
    return rol ? lista.filter((u) => u.roles.includes(rol)) : lista;
  });
  protected readonly tabla = createTableState(this.filas, {
    buscarEn: (u) => [u.username, u.areaNombre],
    ordenarPor: { username: (u) => u.username.toLowerCase() },
    ordenInicial: 'username',
    dirInicial: 'asc',
  });

  protected readonly formAbierto = signal(false);
  protected readonly form = inject(NonNullableFormBuilder).group({
    username: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50), Validators.pattern(PATRONES.username)]],
    password: ['', [Validators.required, Validators.pattern(PATRONES.password)]],
    rol: this.fbRol(),
    areaId: [''],
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly cambiando = signal<number | null>(null);
  protected readonly errorLista = computed(() => leerError(this.lista.error()).mensaje);

  private fbRol() {
    return inject(NonNullableFormBuilder).control<'ADMIN' | 'OPERADOR'>('OPERADOR', Validators.required);
  }

  protected abrir(): void {
    this.form.reset({ username: '', password: '', rol: 'OPERADOR', areaId: '' });
    this.error.set(null);
    this.formAbierto.set(true);
  }

  protected guardar(): void {
    const v = this.form.getRawValue();
    const faltaArea = v.rol === 'OPERADOR' && !v.areaId;
    if (faltaArea) {
      this.form.controls.areaId.setErrors({ required: true });
      this.form.controls.areaId.markAsTouched();
    }
    if (this.form.invalid || faltaArea) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.api
      .crear({ username: v.username.trim(), password: v.password, rol: v.rol, areaId: v.rol === 'OPERADOR' ? +v.areaId : null })
      .subscribe({
        next: (u) => {
          this.toast.success(`Usuario ${u.username} creado.`);
          this.guardando.set(false);
          this.formAbierto.set(false);
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

  protected async alternar(u: UsuarioResponse): Promise<void> {
    if (u.habilitado) {
      const ok = await this.confirm.preguntar({
        titulo: 'Deshabilitar cuenta',
        mensaje: `${u.username} no podrá iniciar sesión hasta que se habilite nuevamente.`,
        confirmar: 'Deshabilitar',
        peligro: true,
      });
      if (!ok) return;
    }
    this.cambiando.set(u.id);
    this.api.cambiarHabilitado(u.id, !u.habilitado).subscribe({
      next: (r) => {
        this.toast.success(r.habilitado ? 'Cuenta habilitada.' : 'Cuenta deshabilitada.');
        this.cambiando.set(null);
        this.lista.reload();
      },
      error: (e) => {
        this.toast.error(leerError(e).mensaje);
        this.cambiando.set(null);
      },
    });
  }
}
