import { Injectable, signal } from '@angular/core';

export type ToastTipo = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  tipo: ToastTipo;
  mensaje: string;
}

/** Mensajes breves de exito/error en la esquina de la pantalla. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private siguienteId = 1;
  readonly toasts = signal<Toast[]>([]);

  success(mensaje: string): void {
    this.mostrar('success', mensaje);
  }

  error(mensaje: string): void {
    this.mostrar('error', mensaje, 6000);
  }

  info(mensaje: string): void {
    this.mostrar('info', mensaje);
  }

  cerrar(id: number): void {
    this.toasts.update((lista) => lista.filter((t) => t.id !== id));
  }

  private mostrar(tipo: ToastTipo, mensaje: string, duracion = 4000): void {
    const id = this.siguienteId++;
    this.toasts.update((lista) => [...lista.slice(-3), { id, tipo, mensaje }]);
    setTimeout(() => this.cerrar(id), duracion);
  }
}
