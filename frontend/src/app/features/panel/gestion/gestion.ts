import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Observable, of } from 'rxjs';
import { AreasApi, CiudadanosApi, IncidenciasApi } from '../../../core/api/apis';
import { SessionService } from '../../../core/auth/session.service';
import { CategoriaIncidencia, EstadoIncidencia, IncidenciaResponse, PrioridadIncidencia } from '../../../core/models/api.models';
import { CATEGORIAS, PRIORIDADES, TRANSICIONES, etiqueta } from '../../../core/models/catalogos';
import { leerError } from '../../../core/services/api-error';
import { ConfirmService } from '../../../core/services/confirm.service';
import { ToastService } from '../../../core/services/toast.service';
import { HistorialTimeline } from '../../../shared/components/historial-timeline';
import { Icon } from '../../../shared/components/icon';
import { IncidenciaFicha } from '../../../shared/components/incidencia-ficha';
import { Loading } from '../../../shared/components/ui';
import { diasDesde } from '../../../shared/utils/agregados';

type Accion = 'estado' | 'area' | 'clasificacion' | 'eliminar';

/**
 * Gestion de una incidencia en el panel:
 *  - PUT /{id}/estado   (ADMIN, OPERADOR de su area; solo ADMIN cierra)
 *  - PUT /{id}/area     (ADMIN)
 *  - PUT /{id}          (ADMIN: reclasificar categoria/prioridad)
 *  - DELETE /{id}       (ADMIN)
 * Cada operacion genera su registro en el historial (lo hace el backend).
 */
@Component({
  selector: 'ua-gestion',
  imports: [RouterLink, Icon, Loading, IncidenciaFicha, HistorialTimeline],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './gestion.html',
})
export class Gestion {
  protected readonly String = String;
  readonly id = input.required<string>();

  private readonly api = inject(IncidenciasApi);
  private readonly areasApi = inject(AreasApi);
  private readonly ciudadanosApi = inject(CiudadanosApi);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  protected readonly session = inject(SessionService);

  protected readonly categorias = CATEGORIAS;
  protected readonly prioridades = PRIORIDADES;

  protected readonly incidencia = rxResource({ params: () => +this.id(), stream: ({ params }) => this.api.obtener(params) });
  protected readonly historial = rxResource({ params: () => +this.id(), stream: ({ params }) => this.api.historial(params) });
  protected readonly areas = rxResource({
    params: () => this.session.puede('INCIDENCIA_ASIGNAR_AREA'),
    stream: ({ params }) => (params ? this.areasApi.listar() : of([])),
  });
  protected readonly ciudadano = rxResource({
    params: () => this.incidencia.value()?.ciudadanoId,
    stream: ({ params }) => this.ciudadanosApi.obtener(params),
  });

  protected readonly enCurso = signal<Accion | null>(null);
  protected readonly nuevoEstado = signal<EstadoIncidencia | ''>('');
  protected readonly nuevaArea = signal('');
  protected readonly nuevaCategoria = signal<CategoriaIncidencia | ''>('');
  protected readonly nuevaPrioridad = signal<PrioridadIncidencia | ''>('');

  protected readonly errorMsg = computed(() => leerError(this.incidencia.error()).mensaje);
  protected readonly cerrada = computed(() => this.incidencia.value()?.estado === 'CERRADA');
  protected readonly antiguedad = computed(() => {
    const i = this.incidencia.value();
    return i ? diasDesde(i.fechaReporte) : 0;
  });

  /** Transiciones validas desde el estado actual; CERRADA solo para ADMIN. */
  protected readonly transiciones = computed(() => {
    const i = this.incidencia.value();
    if (!i) return [];
    return TRANSICIONES[i.estado]
      .filter((e) => e !== 'CERRADA' || this.session.tieneRol('ADMIN'))
      .map((value) => ({ value, label: etiqueta('estado', value) }));
  });

  protected readonly requiereArea = computed(
    () => this.nuevoEstado() === 'EN_PROCESO' && !this.incidencia.value()?.areaResponsableId,
  );

  protected readonly clasificacionCambiada = computed(() => {
    const i = this.incidencia.value();
    return !!i && (this.nuevaCategoria() !== i.categoria || this.nuevaPrioridad() !== i.prioridad);
  });

  constructor() {
    // Sincroniza los controles con la incidencia cargada o actualizada.
    effect(() => {
      const i = this.incidencia.value();
      if (!i) return;
      this.nuevoEstado.set(TRANSICIONES[i.estado][0] ?? '');
      this.nuevaArea.set(i.areaResponsableId ? String(i.areaResponsableId) : '');
      this.nuevaCategoria.set(i.categoria);
      this.nuevaPrioridad.set(i.prioridad);
    });
  }

  protected async cambiarEstado(): Promise<void> {
    const estado = this.nuevoEstado();
    if (!estado) return;
    if (estado === 'CERRADA') {
      const ok = await this.confirm.preguntar({
        titulo: 'Cerrar incidencia',
        mensaje: 'Una incidencia cerrada ya no admite cambios. ¿Desea continuar?',
        confirmar: 'Cerrar incidencia',
      });
      if (!ok) return;
    }
    this.ejecutar('estado', this.api.cambiarEstado(+this.id(), estado), `Estado actualizado a "${etiqueta('estado', estado)}".`);
  }

  protected asignarArea(): void {
    if (!this.nuevaArea()) return;
    this.ejecutar('area', this.api.asignarArea(+this.id(), +this.nuevaArea()), 'Área responsable asignada.');
  }

  protected guardarClasificacion(): void {
    const i = this.incidencia.value();
    const categoria = this.nuevaCategoria();
    const prioridad = this.nuevaPrioridad();
    if (!i || !categoria || !prioridad) return;
    // PUT /{id} exige el cuerpo completo: se reenvian titulo, descripcion y direccion sin cambios.
    this.ejecutar(
      'clasificacion',
      this.api.actualizar(i.id, {
        titulo: i.titulo,
        descripcion: i.descripcion,
        direccionReferencial: i.direccionReferencial,
        categoria,
        prioridad,
      }),
      'Clasificación actualizada.',
    );
  }

  protected async eliminar(): Promise<void> {
    const ok = await this.confirm.preguntar({
      titulo: `Eliminar incidencia #${this.id()}`,
      mensaje: 'Se eliminará el reporte junto con todo su historial. Esta acción no se puede deshacer.',
      confirmar: 'Eliminar',
      peligro: true,
    });
    if (!ok) return;
    this.enCurso.set('eliminar');
    this.api.eliminar(+this.id()).subscribe({
      next: () => {
        this.toast.success('Incidencia eliminada.');
        this.router.navigate(['/panel/incidencias']);
      },
      error: (e) => {
        this.toast.error(leerError(e).mensaje);
        this.enCurso.set(null);
      },
    });
  }

  private ejecutar(accion: Accion, peticion: Observable<IncidenciaResponse>, exito: string): void {
    this.enCurso.set(accion);
    peticion.subscribe({
      next: (actualizada) => {
        this.incidencia.set(actualizada);
        this.historial.reload();
        this.enCurso.set(null);
        this.toast.success(exito);
      },
      error: (e) => {
        this.enCurso.set(null);
        this.toast.error(leerError(e).mensaje);
      },
    });
  }
}
