package com.example.qms.domain.ticket.aggregate.root;

import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.ticket.aggregate.vo.QueueToken;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import com.example.qms.domain.ticket.aggregate.vo.TicketId;
import com.example.qms.domain.ticket.aggregate.vo.TicketStatus;
import com.example.qms.domain.ticket.event.TicketDequeuedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 排隊號碼牌聚合根 (Aggregate Root)
 * 負責管理號碼牌的生命週期、狀態變更以及發布領域事件。
 */
public class Ticket {

    /** 號碼牌唯一識別 */
    private final TicketId ticketId;
    
    /** 租戶識別 */
    private final TenantId tenantId;
    
    /** 活動識別 */
    private final ActivityId activityId;
    
    /** 排隊號碼 (通常為遞增數字) */
    private final long ticketNumber;
    
    /** 查詢進度用的安全權杖 (Queue Token) */
    private final QueueToken queueToken;
    
    /** 號碼牌當前狀態 */
    private TicketStatus status;
    
    /** 建立時間 */
    private final Instant createdAt;
    
    /** 放行時間 */
    private Instant dequeuedAt;

    /** 領域事件清單，用於非同步通知其他聚合或微服務 */
    private final List<Object> domainEvents = new ArrayList<>();

    /**
     * 新建一張號碼牌
     * 
     * @param tenantId 租戶識別
     * @param activityId 活動識別
     * @param ticketNumber 取得的排隊號碼
     */
    public Ticket(TenantId tenantId, ActivityId activityId, long ticketNumber) {
        this.ticketId = new TicketId(UUID.randomUUID().toString());
        this.tenantId = tenantId;
        this.activityId = activityId;
        this.ticketNumber = ticketNumber;
        this.queueToken = new QueueToken(UUID.randomUUID().toString());
        this.status = TicketStatus.QUEUING;
        this.createdAt = Instant.now();
    }

    /**
     * 從資料庫重新建構號碼牌聚合根
     */
    private Ticket(TicketId ticketId, TenantId tenantId, ActivityId activityId, long ticketNumber, QueueToken queueToken, TicketStatus status, Instant createdAt, Instant dequeuedAt) {
        this.ticketId = ticketId;
        this.tenantId = tenantId;
        this.activityId = activityId;
        this.ticketNumber = ticketNumber;
        this.queueToken = queueToken;
        this.status = status;
        this.createdAt = createdAt;
        this.dequeuedAt = dequeuedAt;
    }

    /**
     * 工廠方法：從持久化狀態還原實例
     */
    public static Ticket reconstitute(TicketId ticketId, TenantId tenantId, ActivityId activityId, long ticketNumber, QueueToken queueToken, TicketStatus status, Instant createdAt, Instant dequeuedAt) {
        return new Ticket(ticketId, tenantId, activityId, ticketNumber, queueToken, status, createdAt, dequeuedAt);
    }

    /**
     * 執行放行操作 (Dequeue)
     * 將狀態改為 DEQUEUED，並產生 TicketDequeuedEvent 領域事件。
     * @throws IllegalStateException 如果號碼牌不在排隊中狀態
     */
    public void dequeue() {
        if (this.status != TicketStatus.QUEUING) {
            throw new IllegalStateException("Only QUEUING tickets can be dequeued.");
        }
        this.status = TicketStatus.DEQUEUED;
        this.dequeuedAt = Instant.now();
        
        this.domainEvents.add(new TicketDequeuedEvent(
                this.ticketId,
                this.tenantId,
                this.activityId,
                this.ticketNumber,
                this.dequeuedAt
        ));
    }

    /**
     * 標記號碼牌為超時無效 (Timeout)
     * 將狀態改為 TIMEOUT。
     * @throws IllegalStateException 如果號碼牌不在排隊中狀態
     */
    public void timeout() {
        if (this.status != TicketStatus.QUEUING) {
            throw new IllegalStateException("Only QUEUING tickets can timeout.");
        }
        this.status = TicketStatus.TIMEOUT;
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // Getter methods (No Lombok as per AGENTS.md)
    public TicketId getTicketId() { return ticketId; }
    public TenantId getTenantId() { return tenantId; }
    public ActivityId getActivityId() { return activityId; }
    public long getTicketNumber() { return ticketNumber; }
    public QueueToken getQueueToken() { return queueToken; }
    public TicketStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDequeuedAt() { return dequeuedAt; }
}
