package com.example.qms.domain.ticket.aggregate.vo;

/**
 * 號碼牌唯一識別 (Value Object)
 * 代表一張排隊號碼牌的全球唯一 ID。
 *
 * @param value 號碼牌 ID 字串 (通常為 UUID)
 */
public record TicketId(String value) {
    
    /**
     * 建構子檢驗：確保值不為空
     */
    public TicketId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TicketId cannot be null or empty");
        }
    }
}
