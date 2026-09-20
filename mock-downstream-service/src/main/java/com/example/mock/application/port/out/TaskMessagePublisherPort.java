package com.example.mock.application.port.out;

import com.example.mock.domain.task.event.DownstreamTaskCompletedEvent;
import java.util.concurrent.CompletableFuture;

/**
 * 【Application Layer】任務訊息發布埠 (Outbound Port)
 *
 * 定義了對外發布任務狀態事件的合約。
 * 在六角形架構中，Application Service 透過此介面將領域事件 (Domain Event) 
 * 交給外部基礎設施 (Infrastructure Adapter) 去實際發送 (如 Kafka)。
 */
public interface TaskMessagePublisherPort {
    
    /**
     * 發布「下游任務完成事件」。
     *
     * 通知外部系統 (即 QMS 排隊系統)，該名使用者 (Ticket) 的業務邏輯已處理完畢，
     * 排隊系統可據此釋放容量並叫號下一位使用者。
     *
     * @param event 下游任務完成的領域事件
     * @return 處理完畢的 CompletableFuture
     */
    CompletableFuture<Void> publishTaskCompletedEvent(DownstreamTaskCompletedEvent event);
}
