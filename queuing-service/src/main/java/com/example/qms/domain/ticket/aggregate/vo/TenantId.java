package com.example.qms.domain.ticket.aggregate.vo;

/**
 * 租戶識別 (Value Object)
 * 用於多租戶架構下的資料隔離。
 *
 * @param value 租戶 ID 字串
 */
public record TenantId(String value) {
    
    /**
     * 建構子檢驗：確保值不為空
     */
    public TenantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or empty");
        }
    }
}
