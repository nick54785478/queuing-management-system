package com.example.mock.application.port.in;

import com.example.mock.application.command.ProcessTaskCommand;
import java.util.concurrent.CompletableFuture;

/**
 * 【Application Layer】處理業務邏輯埠 (Inbound Port / Use Case)
 *
 * 定義了 Mock 服務處理虛擬下游業務的核心合約。
 * 當前端呼叫「執行後續業務」時，會觸發此 UseCase，負責模擬耗時處理並於完成後釋放容量。
 */
public interface ProcessTaskUseCase {
    
    /**
     * 執行處理下游業務邏輯。
     * 
     * 此為非同步操作，實作上應模擬業務處理延遲，並在處理完畢後發布
     * DownstreamTaskCompletedEvent，以通知 QMS 釋放系統容量。
     *
     * @param command 包含租戶、活動與號碼牌資訊的命令物件
     * @return 處理完畢的 CompletableFuture
     */
    CompletableFuture<Void> processTask(ProcessTaskCommand command);
}
