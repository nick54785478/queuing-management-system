package com.example.qms.domain.ticket.aggregate.vo;

/**
 * 活動識別 (Value Object)
 * 用於區分不同活動的排隊隊列。
 *
 * @param value 活動 ID 字串
 */
public record ActivityId(String value) {
    
    /**
     * 建構子檢驗：確保值不為空
     */
    public ActivityId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ActivityId cannot be null or empty");
        }
    }
}
