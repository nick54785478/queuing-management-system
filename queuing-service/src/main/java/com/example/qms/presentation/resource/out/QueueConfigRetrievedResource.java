package com.example.qms.presentation.resource.out;

public class QueueConfigRetrievedResource {
    private String tenantId;
    private String activityId;
    private int dequeueBatchSize;
    private String mode;
    private boolean resetDaily;

    public QueueConfigRetrievedResource() {}

    public QueueConfigRetrievedResource(String tenantId, String activityId, int dequeueBatchSize, String mode, boolean resetDaily) {
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

    public boolean isResetDaily() {
        return resetDaily;
    }

    public void setResetDaily(boolean resetDaily) {
        this.resetDaily = resetDaily;
    }
}
