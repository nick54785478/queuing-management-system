package com.example.qms.application.port.in;

import com.example.qms.application.command.ReleaseCapacityCommand;
import java.util.concurrent.CompletableFuture;

/**
 * 釋放 (補充) 放行額度的使用案例 (Use Case) - Inbound Port
 * <p>
 * 此介面為系統內部處理容量回補的核心合約，主要由 Kafka Consumer 
 * (如 KafkaCapacityEventConsumer) 批次聚合下游完成事件後呼叫，
 * 負責將下游已釋放的處理量能 (額度) 實際寫回底層儲存庫 (如 Redis)，
 * 確保排隊系統的放行機制能精準跟隨下游的真實吞吐量。
 * </p>
 */
public interface ReleaseCapacityUseCase {

    /**
     * 執行釋放 (增加) 容量額度的操作
     *
     * @param command 包含租戶、活動及本次欲釋放之總數的命令物件
     * @return CompletableFuture 代表非同步執行之狀態
     */
    CompletableFuture<Void> releaseCapacity(ReleaseCapacityCommand command);
}
