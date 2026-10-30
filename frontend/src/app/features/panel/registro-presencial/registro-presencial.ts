import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AreasApi, CiudadanosApi, IncidenciasApi } from '../../../core/api/apis';
import { CiudadanoResponse } from '../../../core/models/api.models';
import { aplicarErroresServidor, leerError } from '../../../core/services/api-error';
import { ToastService } from '../../../core/services/toast.service';
import { Icon } from '../../../shared/components/icon';
import { IncidenciaCampos, crearIncidenciaForm, valorIncidencia } from '../../../shared/components/incidencia-campos';
import { Loading, PageHeader } from '../../../shared/components/ui';
import { CiudadanoForm } from '../ciudadanos/ciudadano-form';

/**
 * Registro de una incidencia por personal municipal a nombre de un vecino
 * (POST /api/incidencias con ciudadanoId obligatorio y areaId opcional).
 */
@Component({
  selector: 'ua-registro-presencial',
  imports: [ReactiveFormsModule, RouterLink, Icon, PageHeader, Loading, IncidenciaCampos, CiudadanoForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './registro-presencial.html',
})
export class RegistroPresencial {
  protected readonly String = String;
  private readonly incidenciasApi = inject(IncidenciasApi);
  private readonly ciudadanosApi = inject(CiudadanosApi);
  private readonly areasApi = inject(AreasApi);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly form = crearIncidenciaForm(inject(NonNullableFormBuilder));
  protected readonly ciudadanos = rxResource({ stream: () => this.ciudadanosApi.listar() });
  protected readonly areas = rxResource({ stream: () => this.areasApi.listar() });

  protected readonly busqueda = signal('');
  protected readonly seleccionado = signal<CiudadanoResponse | null>(null);
  protected readonly nuevoCiudadano = signal(false);
  protected readonly areaId = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly faltaCiudadano = signal(false);

  protected readonly coincidencias = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    if (q.length < 2) return [];
    return (this.ciudadanos.value() ?? [])
      .filter((c) => c.nombre.toLowerCase().includes(q) || c.dni.includes(q))
      .slice(0, 8);
  });

  constructor() {
    // Preselecciona el ciudadano si se llega desde la lista (?ciudadano=ID).
    const preId = Number(inject(ActivatedRoute).snapshot.queryParamMap.get('ciudadano'));
    if (preId) {
      effect(() => {
        const c = this.ciudadanos.value()?.find((x) => x.id === preId);
        if (c && !this.seleccionado()) this.seleccionado.set(c);
      });
    }
  }

  protected elegir(c: CiudadanoResponse): void {
    this.seleccionado.set(c);
    this.busqueda.set('');
    this.faltaCiudadano.set(false);
  }

  protected creado(c: CiudadanoResponse): void {
    this.toast.success(`Ciudadano ${c.nombre} registrado.`);
    this.nuevoCiudadano.set(false);
    this.ciudadanos.reload();
    this.elegir(c);
  }

  protected guardar(): void {
    const ciudadano = this.seleccionado();
    this.faltaCiudadano.set(!ciudadano);
    if (this.form.invalid || !ciudadano) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.incidenciasApi
      .registrar({ ...valorIncidencia(this.form), ciudadanoId: ciudadano.id, areaId: this.areaId() ? +this.areaId() : null })
      .subscribe({
        next: (i) => {
          this.toast.success(`Incidencia #${i.id} registrada.`);
          this.router.navigate(['/panel/incidencias', i.id]);
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
