package com.example.qms.application.port.out;

import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import com.example.qms.domain.ticket.aggregate.vo.CapacityState;

import java.util.concurrent.CompletableFuture;

/**
 * 處理「放行額度 (Capacity)」與「令牌桶 (Token Bucket)」的狀態儲存庫
 */
public interface CapacityRepositoryPort {

    /**
     * 嘗試扣減並獲取可用的放行額度
     *
     * @param tenantId      租戶 ID
     * @param activityId    活動 ID
     * @param requestedAmount 想要扣減的數量
     * @return 實際扣減成功並取得的數量 (0 ~ requestedAmount)
     */
    CompletableFuture<Integer> tryAcquireCapacity(TenantId tenantId, ActivityId activityId, int requestedAmount);

    /**
     * 釋放 (增加) 額外可用的放行額度，由下游微服務事件觸發
     *
     * @param tenantId      租戶 ID
     * @param activityId    活動 ID
     * @param releasedAmount 要增加的數量
     * @return 非同步結果
     */
    CompletableFuture<Void> releaseCapacity(TenantId tenantId, ActivityId activityId, int releasedAmount);

    CompletableFuture<CapacityState> getCapacity(TenantId tenantId, ActivityId activityId);

    CompletableFuture<Void> setCapacity(TenantId tenantId, ActivityId activityId, int maxCapacity);
    
    /**
     * 從 active_sessions 中移除指定的號碼牌，若移除成功，則釋放指定的容量
     * @return 實際是否執行了釋放 (true: 成功移除並釋放, false: 該號碼已不在 active_sessions 中)
     */
    CompletableFuture<Boolean> removeActiveSessionAndRelease(TenantId tenantId, ActivityId activityId, long ticketNumber, int releasedAmount, String dateSuffix);
}
