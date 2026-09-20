package com.example.qms.domain.ticket.event;

import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import com.example.qms.domain.ticket.aggregate.vo.TicketId;
import java.time.Instant;

/**
 * 號碼牌放行領域事件 (Domain Event)
 * 當一張號碼牌的狀態變更為 DEQUEUED 時觸發，通常用於通知下游系統 (例如發送 WebSocket 廣播通知前端)。
 *
 * @param ticketId 號碼牌唯一識別
 * @param tenantId 租戶識別
 * @param activityId 活動識別
 * @param ticketNumber 放行的排隊號碼
 * @param dequeuedAt 實際放行的時間戳
 */
public record TicketDequeuedEvent(
    TicketId ticketId,
    TenantId tenantId,
    ActivityId activityId,
    long ticketNumber,
    Instant dequeuedAt
) {}
