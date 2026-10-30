import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ConfirmService } from '../../core/services/confirm.service';
import { ToastService } from '../../core/services/toast.service';
import { Icon } from './icon';

@Component({
  selector: 'ua-toasts',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="toasts" aria-live="polite">
      @for (t of toast.toasts(); track t.id) {
        <div class="toast" [class]="'toast toast-' + t.tipo" role="status">
          <ua-icon [name]="t.tipo === 'success' ? 'check' : t.tipo === 'error' ? 'alert' : 'info'" />
          <span>{{ t.mensaje }}</span>
          <button type="button" class="btn-icon btn-ghost" (click)="toast.cerrar(t.id)" aria-label="Cerrar">
            <ua-icon name="x" [size]="14" />
          </button>
        </div>
      }
    </div>
  `,
})
export class Toasts {
  protected readonly toast = inject(ToastService);
}

@Component({
  selector: 'ua-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'confirm.actual() && confirm.responder(false)' },
  template: `
    @if (confirm.actual(); as c) {
      <div class="modal-backdrop" (click)="confirm.responder(false)"></div>
      <div class="modal" role="alertdialog" aria-modal="true" [attr.aria-label]="c.titulo">
        <h2>{{ c.titulo }}</h2>
        <p>{{ c.mensaje }}</p>
        <div class="modal-actions">
          <button type="button" class="btn btn-secondary" (click)="confirm.responder(false)">Cancelar</button>
          <button type="button" class="btn" [class.btn-danger]="c.peligro" [class.btn-primary]="!c.peligro" (click)="confirm.responder(true)">
            {{ c.confirmar ?? 'Confirmar' }}
          </button>
        </div>
      </div>
    }
  `,
})
export class ConfirmDialog {
  protected readonly confirm = inject(ConfirmService);
}
