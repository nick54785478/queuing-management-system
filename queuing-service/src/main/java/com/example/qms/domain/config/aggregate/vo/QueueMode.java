package com.example.qms.domain.config.aggregate.vo;

/**
 * 排隊放行模式列舉 (Value Object)
 */
public enum QueueMode {
    
    /**
     * 自動放行模式：依照設定的批次大小與時間間隔自動叫號
     */
    AUTOMATIC,
    
    /**
     * 手動放行模式：由操作員手動觸發叫號
     */
    MANUAL
}
