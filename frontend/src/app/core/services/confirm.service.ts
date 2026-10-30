import { Injectable, signal } from '@angular/core';

export interface ConfirmOptions {
  titulo: string;
  mensaje: string;
  confirmar?: string;
  peligro?: boolean;
}

interface ConfirmState extends ConfirmOptions {
  resolver: (ok: boolean) => void;
}

/** Dialogo de confirmacion para acciones irreversibles (eliminar, deshabilitar, cerrar). */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly actual = signal<ConfirmState | null>(null);

  preguntar(opciones: ConfirmOptions): Promise<boolean> {
    this.actual()?.resolver(false);
    return new Promise((resolver) => this.actual.set({ ...opciones, resolver }));
  }

  responder(ok: boolean): void {
    this.actual()?.resolver(ok);
    this.actual.set(null);
  }
}
