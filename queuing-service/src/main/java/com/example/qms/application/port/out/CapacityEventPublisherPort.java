package com.example.qms.application.port.out;

import java.util.concurrent.CompletableFuture;

/**
 * 發布釋放容量事件的外部輸出埠 (Outbound Port)
 * <p>
 * 此介面定義了應用層對外發送「任務完成事件 (Task Completed Event)」的基礎合約。
 * 實作此介面的 Adapter (如 KafkaCapacityEventPublisherAdapter) 負責將
 * 釋放額度的請求發送到實際的 Message Broker (例如 Kafka) 中。
 * </p>
 */
public interface CapacityEventPublisherPort {

    /**
     * 非同步發送任務完成事件
     *
     * @param tenantId      租戶識別碼
     * @param activityId    活動識別碼
     * @param ticketNumber  號碼牌號碼
     * @param completedCount 本次已處理完成的任務數量，代表將要釋放回 Redis 的額度
     * @return CompletableFuture 代表非同步發布作業的完成狀態
     */
    CompletableFuture<Void> publishTaskCompletedEvent(String tenantId, String activityId, long ticketNumber, int completedCount);
}
