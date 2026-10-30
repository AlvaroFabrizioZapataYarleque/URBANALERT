import { CategoriaIncidencia, EstadoIncidencia, IncidenciaResponse, PrioridadIncidencia } from '../../core/models/api.models';
import { CATEGORIAS, ESTADOS, PRIORIDADES } from '../../core/models/catalogos';
import { BarDatum } from '../components/ui';

/** Agregaciones en el cliente sobre listados reales (para cortes que /api/reportes no ofrece). */

export const TONO_ESTADO: Record<EstadoIncidencia, string> = {
  REGISTRADA: 'tone-blue',
  EN_PROCESO: 'tone-amber',
  RESUELTA: 'tone-green',
  CERRADA: 'tone-slate',
};

export const TONO_PRIORIDAD: Record<PrioridadIncidencia, string> = {
  BAJA: 'tone-slate',
  MEDIA: 'tone-blue',
  ALTA: 'tone-amber',
  CRITICA: 'tone-red',
};

export const RANGO_PRIORIDAD: Record<PrioridadIncidencia, number> = { BAJA: 1, MEDIA: 2, ALTA: 3, CRITICA: 4 };
export const RANGO_ESTADO: Record<EstadoIncidencia, number> = { REGISTRADA: 1, EN_PROCESO: 2, RESUELTA: 3, CERRADA: 4 };

export function barrasEstado(conteo: Partial<Record<EstadoIncidencia, number>>): BarDatum[] {
  return ESTADOS.map((e) => ({ etiqueta: e.label, valor: conteo[e.value] ?? 0, tono: TONO_ESTADO[e.value] }));
}

export function barrasCategoria(conteo: Partial<Record<CategoriaIncidencia, number>>, sufijoOrden = true): BarDatum[] {
  const datos = CATEGORIAS.map((c) => ({ etiqueta: c.label, valor: conteo[c.value] ?? 0 }));
  return sufijoOrden ? datos.sort((a, b) => b.valor - a.valor) : datos;
}

export function barrasPrioridad(conteo: Partial<Record<PrioridadIncidencia, number>>): BarDatum[] {
  return [...PRIORIDADES].reverse().map((p) => ({ etiqueta: p.label, valor: conteo[p.value] ?? 0, tono: TONO_PRIORIDAD[p.value] }));
}

export function contarPor<K extends string>(lista: IncidenciaResponse[], clave: (i: IncidenciaResponse) => K): Partial<Record<K, number>> {
  const r: Partial<Record<K, number>> = {};
  for (const i of lista) {
    const k = clave(i);
    r[k] = (r[k] ?? 0) + 1;
  }
  return r;
}

/** Dias transcurridos desde una fecha ISO local (LocalDateTime). */
export function diasDesde(fechaIso: string): number {
  return Math.max(0, Math.floor((Date.now() - new Date(fechaIso).getTime()) / 86_400_000));
}

/** Fecha local yyyy-MM-dd (formato que espera IncidenciaFiltro). */
export function fechaIso(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}
