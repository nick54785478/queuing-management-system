import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OperatorService } from '../../services/operator.service';

/**
 * 櫃台操作員的儀表板元件 (Operator Dashboard)
 * 提供給門市櫃台人員或叫號人員使用的專屬介面，主要支援「手動叫號 / 放行」功能。
 */
@Component({
  selector: 'app-operator-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './operator-dashboard.component.html',
  styleUrl: './operator-dashboard.component.css'
})
export class OperatorDashboardComponent {
  /** 標記目前是否正在發送叫號請求，用於禁用按鈕以防止連點 (Button Disabled state) */
  isDequeuing: boolean = false;
  /** 控制成功訊息的顯示與隱藏 (Toast notification state) */
  successMessage: boolean = false;

  constructor(private operatorService: OperatorService) {}

  /**
   * 手動放行 (Manual Dequeue)
   * 當櫃台人員點擊「呼叫下一位」時觸發，向後端發送 HTTP POST 請求。
   * 此方法具有防連點保護與冪等性錯誤 (409 Conflict) 攔截。
   */
  manualDequeue() {
    this.isDequeuing = true;
    this.operatorService.manualDequeue().subscribe({
      next: () => {
        this.isDequeuing = false;
        this.successMessage = true;
        // 顯示成功提示 3 秒後自動消失
        setTimeout(() => this.successMessage = false, 3000);
      },
      error: (err) => {
        if (err.status === 409) {
          // 409 代表此請求的 Idempotency-Key 已經被處理過，屬於重複發送
          console.warn('Duplicate manual dequeue request prevented.');
        } else {
          console.error('Failed to dequeue manually', err);
        }
        this.isDequeuing = false;
      }
    });
  }
}

