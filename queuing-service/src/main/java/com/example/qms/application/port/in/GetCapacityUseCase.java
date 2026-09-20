package com.example.qms.application.port.in;

import com.example.qms.application.query.GetCapacityQuery;

import java.util.concurrent.CompletableFuture;

import com.example.qms.domain.ticket.aggregate.vo.CapacityState;

/**
 * 【Inbound Port】查詢目前系統容量埠 (Use Case)
 * 
 * 負責處理查詢下游系統目前剩餘容量與最大容量的業務邏輯。
 * 供管理員儀表板即時監控系統水位使用。
 */
public interface GetCapacityUseCase {

    /**
     * 查詢指定租戶與活動的目前容量狀態。
     *
     * @param query 包含租戶與活動識別碼的查詢物件
     * @return 包含目前剩餘容量與最大總容量的 CapacityState
     */
    CompletableFuture<CapacityState> getCapacity(GetCapacityQuery query);
}
