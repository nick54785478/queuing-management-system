package com.example.qms.application.port.in;

import com.example.qms.application.dto.QueueConfigGottenResult;
import com.example.qms.application.query.GetQueueConfigQuery;
import java.util.concurrent.CompletableFuture;

/**
 * 【Inbound Port】取得排隊設定的 UseCase
 * 
 * 定義 Worker 或前端查詢特定活動之排隊參數的合約。
 */
public interface GetQueueConfigUseCase {
    CompletableFuture<QueueConfigGottenResult> getQueueConfig(GetQueueConfigQuery query);
}
