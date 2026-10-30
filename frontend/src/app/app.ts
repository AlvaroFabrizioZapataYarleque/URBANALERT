import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialog, Toasts } from './shared/components/overlays';

@Component({
  selector: 'ua-root',
  imports: [RouterOutlet, Toasts, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<router-outlet /><ua-toasts /><ua-confirm-dialog />`,
})
export class App {}
