package com.example.qms.application.port.in;

import com.example.qms.application.command.PingTicketHeartbeatCommand;
import java.util.concurrent.CompletableFuture;

/**
 * 【Inbound Port】號碼牌心跳維持 (Ping) 的 UseCase
 * 
 * 定義了使用者在排隊期間定時發送心跳以維持狀態的業務合約。
 * 若使用者斷線且未發送心跳超過 TTL，將會被移出排隊佇列。
 */
public interface PingTicketHeartbeatUseCase {

    /**
     * @param command 包含租戶、活動及 Queue Token 的命令
     * @return CompletableFuture<Void> 表示處理完成
     */
    CompletableFuture<Void> ping(PingTicketHeartbeatCommand command);
}
