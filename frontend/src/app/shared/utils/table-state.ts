import { Signal, computed, signal } from '@angular/core';

export type SortDir = 'asc' | 'desc';

/**
 * Busqueda, ordenamiento y paginacion en el cliente sobre un listado real de la API
 * (el backend devuelve listas completas, sin Pageable).
 */
export function createTableState<T>(
  fuente: Signal<T[]>,
  opciones: {
    buscarEn: (fila: T) => (string | number | null | undefined)[];
    ordenarPor: Record<string, (fila: T) => string | number>;
    ordenInicial: string;
    dirInicial?: SortDir;
    tamano?: number;
  },
) {
  const texto = signal('');
  const orden = signal(opciones.ordenInicial);
  const dir = signal<SortDir>(opciones.dirInicial ?? 'desc');
  const pagina = signal(1);
  const tamano = signal(opciones.tamano ?? 10);

  const filtradas = computed(() => {
    const q = normalizar(texto().trim());
    const filas = fuente();
    if (!q) return filas;
    return filas.filter((f) => opciones.buscarEn(f).some((v) => v != null && normalizar(String(v)).includes(q)));
  });

  const ordenadas = computed(() => {
    const clave = opciones.ordenarPor[orden()];
    if (!clave) return filtradas();
    const factor = dir() === 'asc' ? 1 : -1;
    return [...filtradas()].sort((a, b) => {
      const va = clave(a);
      const vb = clave(b);
      return (va < vb ? -1 : va > vb ? 1 : 0) * factor;
    });
  });

  const total = computed(() => ordenadas().length);
  const paginaSegura = computed(() => Math.min(pagina(), Math.max(1, Math.ceil(total() / tamano()))));
  const visibles = computed(() => {
    const inicio = (paginaSegura() - 1) * tamano();
    return ordenadas().slice(inicio, inicio + tamano());
  });

  return {
    texto,
    orden,
    dir,
    pagina: paginaSegura,
    tamano,
    total,
    visibles,
    buscar(valor: string) {
      texto.set(valor);
      pagina.set(1);
    },
    ordenar(clave: string) {
      if (orden() === clave) dir.update((d) => (d === 'asc' ? 'desc' : 'asc'));
      else {
        orden.set(clave);
        dir.set('asc');
      }
    },
    irA(n: number) {
      pagina.set(n);
    },
    cambiarTamano(n: number) {
      tamano.set(n);
      pagina.set(1);
    },
  };
}

function normalizar(s: string): string {
  return s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}
