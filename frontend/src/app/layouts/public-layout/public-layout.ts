import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { ReportesApi } from '../../core/api/apis';
import { CATEGORIAS } from '../../core/models/catalogos';
import { Icon } from '../../shared/components/icon';

/** Layout publico (login y registro). Muestra cifras abiertas de GET /api/publico/resumen. */
@Component({
  selector: 'ua-public-layout',
  imports: [RouterOutlet, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="auth">
      <section class="auth-aside">
        <div class="auth-brand">
          <span class="brand-mark"><ua-icon name="pin" [size]="16" [stroke]="2" /></span>
          <strong>UrbanAlert</strong>
        </div>
        <div class="auth-copy">
          <h2>Gestión y seguimiento de incidencias urbanas</h2>
          <p>
            Reporte problemas de alumbrado, pistas, veredas, semáforos, residuos y señalización, y siga su atención
            hasta la resolución.
          </p>
        </div>

        @if (resumen.value(); as r) {
          <div class="auth-stats">
            <div>
              <strong>{{ r.total }}</strong>
              <span>Incidencias reportadas</span>
            </div>
            <div>
              <strong>{{ r.porEstado.EN_PROCESO }}</strong>
              <span>En atención</span>
            </div>
            <div>
              <strong>{{ r.porEstado.RESUELTA + r.porEstado.CERRADA }}</strong>
              <span>Resueltas</span>
            </div>
          </div>
          @if (categoriaTop(); as top) {
            <p class="auth-note">Categoría más reportada: {{ top }}</p>
          }
        }
        <span class="auth-foot">Plataforma municipal de atención ciudadana</span>
      </section>

      <section class="auth-main">
        <router-outlet />
      </section>
    </div>
  `,
})
export class PublicLayout {
  private readonly api = inject(ReportesApi);
  protected readonly resumen = rxResource({ stream: () => this.api.resumenPublico() });

  protected readonly categoriaTop = computed(() => {
    const r = this.resumen.value();
    if (!r || r.total === 0) return null;
    const [clave] = Object.entries(r.porCategoria).sort((a, b) => b[1] - a[1])[0] ?? [];
    return CATEGORIAS.find((c) => c.value === clave)?.label ?? null;
  });
}
