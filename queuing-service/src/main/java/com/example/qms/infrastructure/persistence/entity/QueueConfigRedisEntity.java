package com.example.qms.infrastructure.persistence.entity;

public class QueueConfigRedisEntity {
    private String tenantId;
    private String activityId;
    private int dequeueBatchSize;
    private String mode;
    private Boolean resetDaily;

    public QueueConfigRedisEntity() {}

    public QueueConfigRedisEntity(String tenantId, String activityId, int dequeueBatchSize, String mode, Boolean resetDaily) {
        this.tenantId = tenantId;
        this.activityId = activityId;
        this.dequeueBatchSize = dequeueBatchSize;
        this.mode = mode;
        this.resetDaily = resetDaily;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public int getDequeueBatchSize() {
        return dequeueBatchSize;
    }

    public void setDequeueBatchSize(int dequeueBatchSize) {
        this.dequeueBatchSize = dequeueBatchSize;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Boolean getResetDaily() {
        return resetDaily;
    }

    public void setResetDaily(Boolean resetDaily) {
        this.resetDaily = resetDaily;
    }
}
