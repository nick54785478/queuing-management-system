package com.example.qms.application.port.in;

import com.example.qms.application.command.EnqueueTicketCommand;
import com.example.qms.application.dto.TicketEnqueuedResult;
import java.util.concurrent.CompletableFuture;

/**
 * 【Inbound Port】抽取號碼牌 (Enqueue) 的 UseCase
 * 
 * 定義了使用者點擊「排隊」按鈕後，後端必須實作的業務合約。
 * 此介面獨立於任何通訊協定 (HTTP, gRPC等)，僅傳遞 Command 與 DTO。
 */
public interface EnqueueTicketUseCase {

    /**
     * 執行取號命令。
     * 
     * @param command 包含租戶及活動識別的命令
     * @return 包含取號結果 (ID, 號碼, Token) 的 DTO
     */
    CompletableFuture<TicketEnqueuedResult> enqueue(EnqueueTicketCommand command);
}
