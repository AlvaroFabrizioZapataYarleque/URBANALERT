import { Pipe, PipeTransform } from '@angular/core';
import { CatalogoTipo, etiqueta } from '../../core/models/catalogos';

/** {{ incidencia.categoria | label: 'categoria' }} → "Alumbrado público" */
@Pipe({ name: 'label' })
export class LabelPipe implements PipeTransform {
  transform(valor: string | null | undefined, tipo: CatalogoTipo): string {
    return etiqueta(tipo, valor);
  }
}
