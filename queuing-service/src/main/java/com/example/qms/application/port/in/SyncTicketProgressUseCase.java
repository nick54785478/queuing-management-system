package com.example.qms.application.port.in;

import com.example.qms.application.dto.TicketProgressGottenResult;
import com.example.qms.application.query.SyncTicketProgressQuery;
import java.util.concurrent.Flow;

/**
 * 【Inbound Port】訂閱排隊進度 (SSE) 的 UseCase
 * 
 * 提供前端建立 Server-Sent Events (SSE) 保持長連線，
 * 應用層會定時推播當前的放行進度 (Current Serving Number) 給前端。
 */
public interface SyncTicketProgressUseCase {

    /**
     * @param query 包含租戶及活動識別的查詢條件
     * @return 包含目前進度的非同步事件流 (Flow.Publisher)
     */
    Flow.Publisher<TicketProgressGottenResult> syncProgress(SyncTicketProgressQuery query);
}
