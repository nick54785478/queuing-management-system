package com.example.qms.application.port.out;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 【Outbound Port】冪等性檢查埠
 * 
 * 供 Application Service 呼叫以檢查特定操作是否已被執行過。
 */
public interface IdempotencyHandlerPort {
    /**
     * 檢查並鎖定指定的冪等 Key。
     * 若該 Key 不存在，則寫入並回傳 true (代表是新請求)；若已存在，則回傳 false。
     *
     * @param key 冪等性 Key
     * @param ttl 鎖定的存活時間
     * @return CompletableFuture<Boolean>，為 true 代表是全新請求
     */
    CompletableFuture<Boolean> checkIdempotency(String key, Duration ttl);
}
