package com.example.qms.application.port.out;

import com.example.qms.domain.ticket.aggregate.root.Ticket;
import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * 【Outbound Port】號碼牌持久化儲存埠 (Repository Port)
 * 
 * 依據六角形架構，此介面代表應用層對外要求基礎設施提供儲存與查詢號碼牌的合約。
 * 此介面的所有方法皆只允許傳遞領域物件 (如 TenantId, ActivityId) 或基本型別，
 * 嚴禁依賴任何 Spring 或資料庫專屬物件。
 */
public interface TicketRepositoryPort {

    /**
     * 發放號碼牌 (Enqueue)。
     * 在高併發場景中，此方法必須確保原子性地為該租戶與活動產生一個不重複且遞增的號碼。
     *
     * @param tenantId   租戶識別 (SaaS隔離用)
     * @param activityId 活動識別 (SaaS隔離用)
     * @return 包含新號碼與等候憑證的 Ticket 聚合根 (CompletableFuture)
     */
    CompletableFuture<Ticket> enqueue(TenantId tenantId, ActivityId activityId, String dateSuffix);

    /**
     * 取得目前叫號進度 (Current Serving Number)。
     * 供前端 SSE 定時輪詢使用，以推播當前處理進度給所有正在排隊的用戶。
     *
     * @param tenantId   租戶識別
     * @param activityId 活動識別
     * @return 當前已放行的最大號碼 (CompletableFuture)
     */
    CompletableFuture<Long> getCurrentServingNumber(TenantId tenantId, ActivityId activityId, String dateSuffix);

    /**
     * 延長號碼牌心跳 TTL (Extend Time-To-Live)。
     * 追蹤已放行或排隊中的號碼牌，當心跳停止時，後端可判斷其為放棄排隊並回收容量。
     *
     * @param tenantId   租戶識別
     * @param activityId 活動識別
     * @param queueToken 該次排隊的專屬憑證
     * @param ticketNumber 號碼牌號碼
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> updateActiveSession(TenantId tenantId, ActivityId activityId, String queueToken, long ticketNumber, String dateSuffix);

    /**
     * 批次放行號碼牌 (Dequeue)。
     * 依據指定數量，將處於等候中最前面的使用者取出，並進入放行狀態。
     *
     * @param tenantId   租戶識別
     * @param activityId 活動識別
     * @param limit      本次預計放行的最大數量
     * @return 被放行的號碼牌聚合根集合 (Flow.Publisher)
     */
    Flow.Publisher<Ticket> dequeue(TenantId tenantId, ActivityId activityId, int limit, String dateSuffix);
}
