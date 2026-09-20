package com.example.qms.application.port.in;

import com.example.qms.application.command.UpdateQueueConfigCommand;
import com.example.qms.application.dto.QueueConfigGottenResult;
import java.util.concurrent.CompletableFuture;

/**
 * 【Inbound Port】更新排隊設定的 UseCase
 * 
 * 定義前端或 Admin 控制台更新指定活動排隊參數的合約。
 */
public interface UpdateQueueConfigUseCase {
    
    /**
     * 執行更新排隊設定的業務邏輯。
     * 會校驗輸入的指令參數，更新領域模型中的設定狀態，並將變更持久化。
     *
     * @param command 包含租戶、活動及欲更新之設定參數的命令物件
     * @return 更新成功後的排隊設定結果 (非同步)
     */
    CompletableFuture<QueueConfigGottenResult> updateQueueConfig(UpdateQueueConfigCommand command);
}
