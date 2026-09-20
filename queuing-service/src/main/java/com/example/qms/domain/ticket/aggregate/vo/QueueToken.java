package com.example.qms.domain.ticket.aggregate.vo;

/**
 * 排隊安全權杖 (Value Object)
 * 用於防止惡意猜測票號查詢進度，類似於密碼。
 *
 * @param value 權杖字串 (通常為 UUID)
 */
public record QueueToken(String value) {
    
    /**
     * 建構子檢驗：確保值不為空
     */
    public QueueToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("QueueToken cannot be null or empty");
        }
    }
}
