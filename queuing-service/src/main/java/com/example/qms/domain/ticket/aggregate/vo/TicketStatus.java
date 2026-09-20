package com.example.qms.domain.ticket.aggregate.vo;

/**
 * 號碼牌狀態列舉 (Value Object)
 */
public enum TicketStatus {
    
    /**
     * 排隊中：號碼牌已發放，正在等待放行
     */
    QUEUING,
    
    /**
     * 已放行：號碼牌已被放行，可進行後續業務處理
     */
    DEQUEUED,
    
    /**
     * 超時：號碼牌過期失效
     */
    TIMEOUT
}
