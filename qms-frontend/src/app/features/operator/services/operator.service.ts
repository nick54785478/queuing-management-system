import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

/**
 * 櫃台操作員服務 (Operator Service)
 * 負責處理與櫃台放行、叫號相關的後端 API 請求。
 */
@Injectable({
  providedIn: 'root'
})
export class OperatorService {
  // TODO: 未來將從 JWT 或後端狀態管理中取得
  private tenantId = 'demo-tenant';
  private activityId = 'demo-activity';

  /** 呼叫後端手動放行 (Dequeue) API 的基礎 URL */
  private get dequeueUrl(): string {
    return `${environment.apiUrl}/tenants/${this.tenantId}/activities/${this.activityId}/tickets/dequeue`;
  }

  constructor(private http: HttpClient) { }
  
  /**
   * 觸發手動放行 (Manual Dequeue)
   * 
   * @param limit - (選填) 本次要放行的數量。如果不傳，後端會自動去抓取 QueueConfig 中設定的批次數量。
   * 
   * @description
   * 為了避免櫃台人員網路延遲導致重複發送放行請求，
   * 每次呼叫此方法都會自動產生一組全新的 `Idempotency-Key` (冪等性金鑰, UUID V4)，
   * 並將其附帶於 Request Header 傳送給後端。後端的 Redis 會紀錄此金鑰，
   * 確保同一個 UUID 在短時間內只會成功執行一次。
   */
  manualDequeue(limit?: number): Observable<void> {
    const idempotencyKey = crypto.randomUUID();
    const url = limit ? `${this.dequeueUrl}?limit=${limit}` : this.dequeueUrl;
    return this.http.post<void>(url, {}, {
      headers: {
        'Idempotency-Key': idempotencyKey
      }
    });
  }
}
