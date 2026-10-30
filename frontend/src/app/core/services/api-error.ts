import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup } from '@angular/forms';
import { ApiErrorBody } from '../models/api.models';

export interface ApiError {
  status: number;
  mensaje: string;
  /** Errores de Bean Validation por campo ({ errores: { campo: mensaje } }). */
  campos: Record<string, string>;
}

/** Traduce cualquier error HTTP al formato del GlobalExceptionHandler del backend. */
export function leerError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { status: 0, mensaje: 'Ocurrió un error inesperado.', campos: {} };
  }
  if (error.status === 0) {
    return {
      status: 0,
      mensaje: 'No se pudo conectar con el servidor. Verifique que la API esté disponible.',
      campos: {},
    };
  }
  const cuerpo = (typeof error.error === 'object' && error.error ? error.error : {}) as ApiErrorBody;
  const campos = cuerpo.errores ?? {};
  const mensaje =
    cuerpo.mensaje ??
    (Object.keys(campos).length ? 'Revise los campos marcados.' : mensajePorEstado(error.status));
  return { status: error.status, mensaje, campos };
}

function mensajePorEstado(status: number): string {
  switch (status) {
    case 400:
      return 'La solicitud no es válida.';
    case 401:
      return 'Su sesión expiró. Inicie sesión nuevamente.';
    case 403:
      return 'No tiene permisos para realizar esta operación.';
    case 404:
      return 'El recurso solicitado no existe.';
    case 409:
      return 'La operación entra en conflicto con datos existentes.';
    default:
      return 'El servidor no pudo completar la operación.';
  }
}

/** Marca en el formulario los errores de validacion que devolvio el servidor (error "server"). */
export function aplicarErroresServidor(form: FormGroup, campos: Record<string, string>): void {
  for (const [campo, mensaje] of Object.entries(campos)) {
    const control: AbstractControl | null = form.get(campo);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), server: mensaje });
      control.markAsTouched();
    }
  }
}
