package com.example.qms.application.port.in;

import com.example.qms.application.command.DequeueTicketCommand;
import java.util.concurrent.CompletableFuture;

/**
 * 【Inbound Port】放行號碼牌 (Dequeue) 的 UseCase
 * 
 * 提供背景 Worker 定時呼叫的合約，依據設定好的放行數量，
 * 批次將等候中的使用者放行。
 */
public interface DequeueTicketUseCase {
    CompletableFuture<Integer> dequeue(DequeueTicketCommand command);
}
