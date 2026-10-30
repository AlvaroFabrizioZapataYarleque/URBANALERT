import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { Icon, IconName } from './icon';

/** Encabezado de pagina: titulo, descripcion y acciones (contenido proyectado). */
@Component({
  selector: 'ua-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-header">
      <div>
        <h1>{{ titulo() }}</h1>
        @if (descripcion()) {
          <p>{{ descripcion() }}</p>
        }
      </div>
      <div class="page-actions"><ng-content /></div>
    </div>
  `,
})
export class PageHeader {
  readonly titulo = input.required<string>();
  readonly descripcion = input<string>();
}

/** Tarjeta de indicador numerico. */
@Component({
  selector: 'ua-stat-card',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stat" [class]="'stat stat-' + tono()">
      <div class="stat-icon"><ua-icon [name]="icono()" [size]="18" /></div>
      <div class="stat-body">
        <span class="stat-label">{{ etiqueta() }}</span>
        <strong class="stat-value">{{ valor() }}</strong>
        @if (detalle()) {
          <span class="stat-detail">{{ detalle() }}</span>
        }
      </div>
    </div>
  `,
})
export class StatCard {
  readonly etiqueta = input.required<string>();
  readonly valor = input.required<string | number>();
  readonly icono = input<IconName>('chart');
  readonly detalle = input<string>();
  readonly tono = input<'blue' | 'amber' | 'green' | 'slate' | 'red'>('blue');
}

export interface BarDatum {
  etiqueta: string;
  valor: number;
  tono?: string;
}

/** Grafico de barras horizontales en CSS (sin librerias). */
@Component({
  selector: 'ua-bar-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (datos().length === 0) {
      <p class="muted small">Sin datos para mostrar.</p>
    } @else {
      <ul class="bars" role="list">
        @for (d of datos(); track d.etiqueta) {
          <li class="bar-row">
            <span class="bar-label" [title]="d.etiqueta">{{ d.etiqueta }}</span>
            <span class="bar-track">
              <span class="bar-fill" [class]="'bar-fill ' + (d.tono ?? 'tone-blue')" [style.width.%]="ancho(d.valor)"></span>
            </span>
            <span class="bar-value">{{ d.valor }}{{ sufijo() }}</span>
          </li>
        }
      </ul>
    }
  `,
})
export class BarChart {
  readonly datos = input.required<BarDatum[]>();
  readonly sufijo = input('');
  private readonly maximo = computed(() => Math.max(1, ...this.datos().map((d) => d.valor)));
  protected ancho(valor: number): number {
    return valor <= 0 ? 0 : Math.max(2, (valor / this.maximo()) * 100);
  }
}

/** Paginador para listados (la API no pagina: se pagina en el cliente). */
@Component({
  selector: 'ua-paginator',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="paginator">
      <span class="muted small">
        @if (total() === 0) {
          0 resultados
        } @else {
          {{ desde() }}–{{ hasta() }} de {{ total() }}
        }
      </span>
      <div class="paginator-controls">
        <label class="small muted">
          Filas
          <select class="input input-sm" [value]="tamano()" (change)="tamanoChange.emit(+$any($event.target).value)">
            @for (op of opciones; track op) {
              <option [value]="op" [selected]="op === tamano()">{{ op }}</option>
            }
          </select>
        </label>
        <button class="btn btn-icon btn-ghost" type="button" [disabled]="pagina() <= 1" (click)="paginaChange.emit(pagina() - 1)" aria-label="Página anterior">
          <ua-icon name="chevron-left" />
        </button>
        <span class="small">{{ pagina() }} / {{ paginas() }}</span>
        <button class="btn btn-icon btn-ghost" type="button" [disabled]="pagina() >= paginas()" (click)="paginaChange.emit(pagina() + 1)" aria-label="Página siguiente">
          <ua-icon name="chevron-right" />
        </button>
      </div>
    </div>
  `,
})
export class Paginator {
  readonly total = input.required<number>();
  readonly pagina = input.required<number>();
  readonly tamano = input.required<number>();
  readonly paginaChange = output<number>();
  readonly tamanoChange = output<number>();
  protected readonly opciones = [10, 25, 50];
  protected readonly paginas = computed(() => Math.max(1, Math.ceil(this.total() / this.tamano())));
  protected readonly desde = computed(() => (this.pagina() - 1) * this.tamano() + 1);
  protected readonly hasta = computed(() => Math.min(this.total(), this.pagina() * this.tamano()));
}

/** Mensaje de error de un control: validaciones locales o error devuelto por el servidor. */
@Component({
  selector: 'ua-field-error',
  changeDetection: ChangeDetectionStrategy.Default,
  template: `
    @if (mensaje(); as m) {
      <span class="field-error">{{ m }}</span>
    }
  `,
})
export class FieldError {
  readonly control = input.required<AbstractControl | null>();
  /** Mensaje para el error "required" (ej. "Ingrese su contraseña."). */
  readonly requerido = input<string>();

  protected mensaje(): string | null {
    const c = this.control();
    if (!c || !c.errors || !(c.touched || c.dirty)) return null;
    const e = c.errors;
    if (e['server']) return e['server'];
    if (e['required']) return this.requerido() ?? 'Este campo es obligatorio.';
    if (e['minlength']) return `Debe tener al menos ${e['minlength'].requiredLength} caracteres.`;
    if (e['maxlength']) return `No puede superar ${e['maxlength'].requiredLength} caracteres.`;
    if (e['email']) return 'Ingrese un correo electrónico válido.';
    if (e['pattern']) return this.patron() ?? 'El formato no es válido.';
    if (e['coincidencia']) return 'Las contraseñas no coinciden.';
    return 'Valor no válido.';
  }

  /** Texto alternativo para errores de patron (ej. "El DNI debe tener 8 dígitos"). */
  readonly patron = input<string>();
}

/** Estado vacio o de error de un listado. */
@Component({
  selector: 'ua-empty',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty">
      <ua-icon [name]="icono()" [size]="28" [stroke]="1.5" />
      <strong>{{ titulo() }}</strong>
      @if (mensaje()) {
        <p>{{ mensaje() }}</p>
      }
      <ng-content />
    </div>
  `,
})
export class EmptyState {
  readonly titulo = input.required<string>();
  readonly mensaje = input<string>();
  readonly icono = input<IconName>('inbox');
}

@Component({
  selector: 'ua-loading',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div class="loading" role="status"><span class="spinner"></span>{{ texto() }}</div>`,
})
export class Loading {
  readonly texto = input('Cargando…');
}
