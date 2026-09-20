package com.example.mock.domain.task.event;

import com.example.mock.domain.task.aggregate.vo.ActivityId;
import com.example.mock.domain.task.aggregate.vo.TenantId;
import com.example.mock.domain.task.aggregate.vo.TicketNumber;

public class DownstreamTaskCompletedEvent {
    private final TenantId tenantId;
    private final ActivityId activityId;
    private final TicketNumber ticketNumber;
    private final int completedCount;

    public DownstreamTaskCompletedEvent(TenantId tenantId, ActivityId activityId, TicketNumber ticketNumber, int completedCount) {
        this.tenantId = tenantId;
        this.activityId = activityId;
        this.ticketNumber = ticketNumber;
        this.completedCount = completedCount;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public ActivityId getActivityId() {
        return activityId;
    }

    public TicketNumber getTicketNumber() {
        return ticketNumber;
    }

    public int getCompletedCount() {
        return completedCount;
    }
}
