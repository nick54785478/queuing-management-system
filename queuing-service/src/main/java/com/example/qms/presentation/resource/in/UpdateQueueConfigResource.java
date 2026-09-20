package com.example.qms.presentation.resource.in;

public class UpdateQueueConfigResource {
    private int dequeueBatchSize;
    private String mode;
    private boolean resetDaily;

    public UpdateQueueConfigResource() {}

    public UpdateQueueConfigResource(int dequeueBatchSize, String mode, boolean resetDaily) {
        this.dequeueBatchSize = dequeueBatchSize;
        this.mode = mode;
        this.resetDaily = resetDaily;
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
