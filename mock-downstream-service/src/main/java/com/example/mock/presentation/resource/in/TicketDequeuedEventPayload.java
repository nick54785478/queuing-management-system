package com.example.mock.presentation.resource.in;

public class TicketDequeuedEventPayload {
    private String tenantId;
    private String activityId;
    private long ticketNumber;
    private String idempotencyKey;
    private String status;
    private String dequeuedAt;
    
    // Getters and Setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getActivityId() { return activityId; }
    public void setActivityId(String activityId) { this.activityId = activityId; }

    public long getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(long ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDequeuedAt() { return dequeuedAt; }
    public void setDequeuedAt(String dequeuedAt) { this.dequeuedAt = dequeuedAt; }
}
