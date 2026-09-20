package com.example.qms.application.port.out;

import com.example.qms.domain.config.aggregate.root.QueueConfig;
import java.util.concurrent.CompletableFuture;

/**
 * 【Outbound Port】排隊設定持久化與快取儲存埠 (Repository Port)
 * 
 * 依據六角形架構，此介面代表應用層對外要求基礎設施提供儲存與查詢排隊設定的合約。
 * 此介面的實作應包含雙寫機制（例如寫入關聯式資料庫並同步至 Redis 快取）。
 * 嚴禁依賴任何 Spring 或資料庫專屬物件。
 */
public interface QueueConfigRepositoryPort {

    /**
     * 儲存排隊設定 (Insert / Update)。
     * 若該租戶與活動的設定已存在則更新，否則新增。
     *
     * @param config 包含更新後參數的 QueueConfig 聚合根
     * @return 儲存成功後的 QueueConfig (CompletableFuture)
     */
    CompletableFuture<QueueConfig> save(QueueConfig config);

    /**
     * 依據租戶與活動識別取得最新的排隊設定。
     * 通常由背景 Worker 頻繁呼叫以取得放行參數，實作上應優先讀取快取。
     *
     * @param tenantId   租戶識別 (SaaS隔離用)
     * @param activityId 活動識別 (SaaS隔離用)
     * @return 該活動的 QueueConfig，若無設定則回傳完成結果為 null 的 CompletableFuture
     */
    CompletableFuture<QueueConfig> findByTenantIdAndActivityId(String tenantId, String activityId);
}
