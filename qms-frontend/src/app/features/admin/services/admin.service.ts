import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UpdateQueueConfigResource, QueueConfigRetrievedResource } from '../models/admin.model';
import { environment } from '../../../../environments/environment';

/**
 * 管理員服務 (Admin Service)
 * 負責處理與系統全域設定 (Queue Config) 相關的後端 API 請求。
 */
@Injectable({
  providedIn: 'root'
})
export class AdminService {
  // TODO: 未來將從 JWT 或後端狀態管理中取得
  private tenantId = 'demo-tenant';
  private activityId = 'demo-activity';

  /** 呼叫後端排隊設定 (Config) API 的基礎 URL */
  private get configUrl(): string {
    return `${environment.apiUrl}/admin/tenants/${this.tenantId}/activities/${this.activityId}/config`;
  }

  private get capacityUrl(): string {
    return `${environment.apiUrl}/capacity/tenants/${this.tenantId}/activities/${this.activityId}`;
  }

  constructor(private http: HttpClient) { }

  /**
   * 取得目前的排隊設定
   * 從後端 Redis 快取或資料庫中撈取當前的放行數量、模式及每日歸零設定。
   */
  getConfig(): Observable<QueueConfigRetrievedResource> {
    return this.http.get<QueueConfigRetrievedResource>(this.configUrl);
  }

  /**
   * 更新排隊設定
   * 
   * @param config - 包含要更新的放行批次大小 (dequeueBatchSize)、模式 (mode) 及每日歸零 (resetDaily) 等設定
   * 
   * @description
   * 更新設定後，後端會將新設定同步寫入 PostgreSQL 資料庫與 Redis 快取中，
   * 確保即時生效且資料不遺失。
   */
  updateConfig(config: UpdateQueueConfigResource): Observable<QueueConfigRetrievedResource> {
    return this.http.put<QueueConfigRetrievedResource>(this.configUrl, config);
  }

  /**
   * 取得目前的可用容量狀態
   */
  getCapacity(): Observable<{ remainingCapacity: number, maxCapacity: number }> {
    return this.http.get<{ remainingCapacity: number, maxCapacity: number }>(this.capacityUrl);
  }

  /**
   * 重置/覆蓋系統最大總容量
   */
  resetCapacity(capacity: number): Observable<void> {
    return this.http.post<void>(`${this.capacityUrl}/reset`, { capacity });
  }
}
