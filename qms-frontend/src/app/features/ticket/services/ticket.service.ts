import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { TicketEnqueuedResource, TicketProgressRetrievedResource } from '../models/ticket.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  // TODO: 未來將從 JWT 或後端狀態管理中取得
  private tenantId = 'demo-tenant';
  private activityId = 'demo-activity';

  private get baseUrl(): string {
    return `${environment.apiUrl}/tenants/${this.tenantId}/activities/${this.activityId}/tickets`;
  }
  
  constructor(private http: HttpClient, private zone: NgZone) {}

  /**
   * 取號
   */
  enqueue(): Observable<TicketEnqueuedResource> {
    // 傳遞空物件做為 body
    return this.http.post<TicketEnqueuedResource>(this.baseUrl, {});
  }

  /**
   * 心跳續命
   */
  pingHeartbeat(queueToken: string, ticketNumber: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/heartbeat`, { queueToken, ticketNumber });
  }

  /**
   * SSE 推播進度
   */
  streamProgress(): Observable<TicketProgressRetrievedResource> {
    return new Observable(observer => {
      const eventSource = new EventSource(`${this.baseUrl}/stream`);

      eventSource.onmessage = (event) => {
        // Run inside Angular zone to trigger change detection
        this.zone.run(() => {
          const data: TicketProgressRetrievedResource = JSON.parse(event.data);
          observer.next(data);
        });
      };

      eventSource.onerror = (error) => {
        this.zone.run(() => {
          console.error('SSE Error:', error);
          // Optional: close or keep retrying
          // observer.error(error);
          // eventSource.close();
        });
      };

      return () => {
        eventSource.close();
      };
    });
  }
}
