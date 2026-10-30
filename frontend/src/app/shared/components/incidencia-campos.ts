import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategoriaIncidencia, PrioridadIncidencia } from '../../core/models/api.models';
import { CATEGORIAS, PRIORIDADES } from '../../core/models/catalogos';
import { FieldError } from './ui';

export type IncidenciaForm = FormGroup<{
  titulo: FormControl<string>;
  descripcion: FormControl<string>;
  categoria: FormControl<CategoriaIncidencia | ''>;
  prioridad: FormControl<PrioridadIncidencia>;
  direccionReferencial: FormControl<string>;
}>;

/** Mismos limites que IncidenciaRequest / IncidenciaActualizacionRequest. */
export function crearIncidenciaForm(fb: NonNullableFormBuilder): IncidenciaForm {
  return fb.group({
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    descripcion: ['', [Validators.required, Validators.maxLength(500)]],
    categoria: fb.control<CategoriaIncidencia | ''>('', Validators.required),
    prioridad: fb.control<PrioridadIncidencia>('MEDIA', Validators.required),
    direccionReferencial: ['', Validators.maxLength(200)],
  });
}

export function valorIncidencia(form: IncidenciaForm) {
  const v = form.getRawValue();
  return {
    titulo: v.titulo.trim(),
    descripcion: v.descripcion.trim(),
    categoria: v.categoria as CategoriaIncidencia,
    prioridad: v.prioridad,
    direccionReferencial: v.direccionReferencial.trim() || null,
  };
}

/** Campos comunes del reporte de una incidencia (portal ciudadano y registro presencial). */
@Component({
  selector: 'ua-incidencia-campos',
  imports: [ReactiveFormsModule, FieldError],
  changeDetection: ChangeDetectionStrategy.Default,
  template: `
    <ng-container [formGroup]="form()">
      <fieldset class="form-section">
        <legend>¿Qué ocurre?</legend>
        <div class="field">
          <label for="titulo">Título</label>
          <input id="titulo" class="input" formControlName="titulo" maxlength="150" placeholder="Ej.: Poste de luz apagado en la esquina" />
          <ua-field-error [control]="form().controls.titulo" requerido="Ingrese un título." />
        </div>
        <div class="field">
          <label for="descripcion">Descripción detallada</label>
          <textarea id="descripcion" class="input" rows="5" formControlName="descripcion" maxlength="500"
            placeholder="Describa el problema, desde cuándo ocurre y cómo afecta a los vecinos."></textarea>
          <span class="hint">{{ form().controls.descripcion.value.length }}/500 caracteres</span>
          <ua-field-error [control]="form().controls.descripcion" requerido="Describa el problema." />
        </div>
      </fieldset>

      <fieldset class="form-section">
        <legend>Clasificación</legend>
        <div class="field">
          <span class="label">Categoría</span>
          <div class="choice-grid" role="radiogroup" aria-label="Categoría">
            @for (c of categorias; track c.value) {
              <label class="choice" [class.selected]="form().controls.categoria.value === c.value">
                <input type="radio" formControlName="categoria" [value]="c.value" />
                <span>{{ c.label }}</span>
              </label>
            }
          </div>
          <ua-field-error [control]="form().controls.categoria" requerido="Seleccione una categoría." />
        </div>
        <div class="field field-narrow">
          <label for="prioridad">{{ etiquetaPrioridad() }}</label>
          <select id="prioridad" class="input" formControlName="prioridad">
            @for (p of prioridades; track p.value) {
              <option [value]="p.value">{{ p.label }}</option>
            }
          </select>
          @if (ayudaPrioridad()) {
            <span class="hint">{{ ayudaPrioridad() }}</span>
          }
        </div>
      </fieldset>

      <fieldset class="form-section">
        <legend>Ubicación</legend>
        <div class="field">
          <label for="direccion">Dirección o referencia <span class="optional">opcional</span></label>
          <input id="direccion" class="input" formControlName="direccionReferencial" maxlength="200"
            placeholder="Ej.: Av. Grau 450, frente al mercado" />
          <ua-field-error [control]="form().controls.direccionReferencial" />
        </div>
      </fieldset>
    </ng-container>
  `,
})
export class IncidenciaCampos {
  readonly form = input.required<IncidenciaForm>();
  readonly etiquetaPrioridad = input('Prioridad');
  readonly ayudaPrioridad = input<string>();
  protected readonly categorias = CATEGORIAS;
  protected readonly prioridades = PRIORIDADES;
}
