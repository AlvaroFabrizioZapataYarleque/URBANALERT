import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { SessionService } from '../../core/auth/session.service';
import { NombreRol } from '../../core/models/api.models';
import { etiqueta } from '../../core/models/catalogos';
import { Icon, IconName } from '../../shared/components/icon';

export interface NavItem {
  ruta: string;
  etiqueta: string;
  icono: IconName;
  /** Si se indica, el enlace solo se muestra a estos roles (control visual; el backend autoriza). */
  roles?: NombreRol[];
  exacto?: boolean;
}

export interface NavGroup {
  titulo?: string;
  items: NavItem[];
}

export interface ShellConfig {
  contexto: string;
  grupos: NavGroup[];
}

/** Layout autenticado: sidebar + barra superior. La configuracion viene en route.data['shell']. */
@Component({
  selector: 'ua-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.html',
})
export class Shell {
  protected readonly session = inject(SessionService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly config = this.route.snapshot.data['shell'] as ShellConfig;
  protected readonly menuAbierto = signal(false);
  protected readonly perfilAbierto = signal(false);

  protected readonly grupos = computed(() =>
    this.config.grupos
      .map((g) => ({ ...g, items: g.items.filter((i) => !i.roles || this.session.tieneRol(...i.roles)) }))
      .filter((g) => g.items.length > 0),
  );

  protected readonly rol = computed(() => etiqueta('rol', this.session.rolPrincipal()));
  protected readonly iniciales = computed(() => this.session.username().slice(0, 2).toUpperCase());

  constructor() {
    // Cierra el menu movil al navegar.
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed()).subscribe(() => {
      this.menuAbierto.set(false);
      this.perfilAbierto.set(false);
    });
  }

  protected salir(): void {
    this.session.cerrar('manual');
  }
}
