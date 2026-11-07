import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Subscription, interval } from 'rxjs';
import { TicketService } from '../../services/ticket.service';
import { ProgressRingComponent } from '../../../../shared/components/progress-ring/progress-ring.component';

@Component({
  selector: 'app-ticket-board',
  standalone: true,
  imports: [CommonModule, ProgressRingComponent],
  templateUrl: './ticket-board.component.html',
  styleUrl: './ticket-board.component.css'
})
export class TicketBoardComponent implements OnDestroy {
  status: 'IDLE' | 'QUEUING' | 'PASSED' | 'IN_SYSTEM' = 'IDLE';
  
  ticketNumber: number = 0;
  currentServingNumber: number = 0;
  queueToken: string = '';
  
  private sseSubscription?: Subscription;
  private heartbeatSubscription?: Subscription;

  constructor(private ticketService: TicketService, private http: HttpClient) {}

  get peopleAhead(): number {
    return Math.max(0, this.ticketNumber - this.currentServingNumber);
  }

  enqueue() {
    this.ticketService.enqueue().subscribe({
      next: (res) => {
        this.status = 'QUEUING';
        this.ticketNumber = res.ticketNumber;
        this.queueToken = res.queueToken;
        this.startSSE();
        this.startHeartbeat();
      },
      error: (err) => console.error('Enqueue failed:', err)
    });
  }

  enterSystem() {
    this.status = 'IN_SYSTEM';
  }

  executeBusiness() {
    // 呼叫 Mock Downstream Service 執行業務流程並釋放容量
    this.http.post('http://localhost:8081/api/mock/business/process', {
      tenantId: 'demo-tenant',
      activityId: 'demo-activity',
      ticketNumber: this.ticketNumber
    }, { responseType: 'text' }).subscribe({
      next: (res) => {
        alert('🎉 業務流程完成！系統已釋放名額給下一位排隊者。');
        this.status = 'IDLE'; // 業務結束後回到初始畫面
        this.stopConnections(); // 業務完成才停止心跳
        this.ticketNumber = 0;
        this.currentServingNumber = 0;
      },
      error: (err) => {
        console.error('Execute Business Failed:', err);
        alert('發生錯誤：無法連接到下游系統');
      }
    });
  }

  private startSSE() {
    this.sseSubscription = this.ticketService.streamProgress().subscribe({
      next: (res) => {
        this.currentServingNumber = res.currentServingNumber;
        if (this.currentServingNumber >= this.ticketNumber && this.status === 'QUEUING') {
          this.status = 'PASSED';
          // 取消 SSE 訂閱，但保留 heartbeatSubscription 繼續發送心跳
          if (this.sseSubscription) {
            this.sseSubscription.unsubscribe();
            this.sseSubscription = undefined;
          }
        }
      }
    });
  }

  private startHeartbeat() {
    // Ping every 10 seconds
    this.heartbeatSubscription = interval(10000).subscribe(() => {
      this.ticketService.pingHeartbeat(this.queueToken, this.ticketNumber).subscribe({
        error: (err) => console.error('Heartbeat failed', err)
      });
    });
  }

  private stopConnections() {
    if (this.sseSubscription) this.sseSubscription.unsubscribe();
    if (this.heartbeatSubscription) this.heartbeatSubscription.unsubscribe();
  }

  ngOnDestroy() {
    this.stopConnections();
  }
}
