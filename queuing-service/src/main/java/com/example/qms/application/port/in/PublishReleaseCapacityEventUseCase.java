package com.example.qms.application.port.in;

import com.example.qms.application.command.PublishReleaseCapacityEventCommand;
import java.util.concurrent.CompletableFuture;

/**
 * 發布釋放容量事件的使用案例 (Use Case) - Inbound Port
 * <p>
 * 此介面為應用層對外提供的服務合約之一，專門提供給不具備 EDA (Event-Driven Architecture)
 * 能力的下游系統透過 HTTP API (如 CapacityController) 來主動通知排隊系統釋放額度。
 * 收到請求後，實作類別會將此請求轉換為領域事件並推送到 Kafka 中，
 * 以便讓 Kafka Consumer 統一進行批次聚合與削峰填谷。
 * </p>
 */
public interface PublishReleaseCapacityEventUseCase {

    /**
     * 發布容量釋放事件
     *
     * @param command 包含租戶、活動及本次完成任務數量的命令物件
     * @return CompletableFuture 代表非同步發布作業的完成狀態
     */
    CompletableFuture<Void> publish(PublishReleaseCapacityEventCommand command);
}
