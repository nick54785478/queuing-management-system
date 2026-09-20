package com.example.qms.domain.config.aggregate.root;

import com.example.qms.domain.config.aggregate.vo.QueueMode;

/**
 * 排隊設定聚合根 (Aggregate Root)
 * 負責維護租戶與活動層級的排隊設定，確保設定的一致性與合法性。
 */
public class QueueConfig {
    
    /** 租戶 ID，用於多租戶隔離 */
    private String tenantId;
    
    /** 活動 ID，對應特定的排隊活動 */
    private String activityId;
    
    /** 每次放行 (Dequeue) 的批次大小 */
    private int dequeueBatchSize;
    
    /** 放行模式 (自動或手動) */
    private QueueMode mode;
    
    /** 是否每日重置排隊號碼與隊列 */
    private boolean resetDaily;

    private QueueConfig() {}

    private QueueConfig(String tenantId, String activityId, int dequeueBatchSize, QueueMode mode, boolean resetDaily) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId cannot be blank");
        }
        if (activityId == null || activityId.isBlank()) {
            throw new IllegalArgumentException("activityId cannot be blank");
        }
        if (dequeueBatchSize < 1) {
            throw new IllegalArgumentException("dequeueBatchSize must be at least 1");
        }
        if (mode == null) {
            throw new IllegalArgumentException("mode cannot be null");
        }
        this.tenantId = tenantId;
        this.activityId = activityId;
        this.dequeueBatchSize = dequeueBatchSize;
        this.mode = mode;
        this.resetDaily = resetDaily;
    }

    /**
     * 創建一個新的排隊設定實例 (用於第一次創建時)。
     * 
     * @param tenantId 租戶 ID
     * @param activityId 活動 ID
     * @param dequeueBatchSize 批次放行大小
     * @param mode 放行模式
     * @param resetDaily 是否每日重置
     * @return 創建後的 QueueConfig
     */
    public static QueueConfig create(String tenantId, String activityId, int dequeueBatchSize, QueueMode mode, boolean resetDaily) {
        return new QueueConfig(tenantId, activityId, dequeueBatchSize, mode, resetDaily);
    }

    /**
     * 重組現有的排隊設定實例 (用於從資料庫載入時)。
     * 
     * @param tenantId 租戶 ID
     * @param activityId 活動 ID
     * @param dequeueBatchSize 批次放行大小
     * @param mode 放行模式
     * @param resetDaily 是否每日重置
     * @return 重組後的 QueueConfig
     */
    public static QueueConfig reconstitute(String tenantId, String activityId, int dequeueBatchSize, QueueMode mode, boolean resetDaily) {
        return new QueueConfig(tenantId, activityId, dequeueBatchSize, mode, resetDaily);
    }


    public String getTenantId() {
        return tenantId;
    }

    public String getActivityId() {
        return activityId;
    }

    public int getDequeueBatchSize() {
        return dequeueBatchSize;
    }
    
    public QueueMode getMode() {
        return mode;
    }

    public boolean isResetDaily() {
        return resetDaily;
    }

    /**
     * 更新批次放行大小。
     * 
     * @param newBatchSize 新的批次大小 (必須大於等於 1)
     */
    public void updateDequeueBatchSize(int newBatchSize) {
        if (newBatchSize < 1) {
            throw new IllegalArgumentException("dequeueBatchSize must be at least 1");
        }
        this.dequeueBatchSize = newBatchSize;
    }
    
    /**
     * 更新放行模式。
     * 
     * @param newMode 新的放行模式
     */
    public void updateMode(QueueMode newMode) {
        if (newMode == null) {
            throw new IllegalArgumentException("mode cannot be null");
        }
        this.mode = newMode;
    }

    /**
     * 更新是否每日重置。
     * 
     * @param resetDaily 是否每日重置
     */
    public void updateResetDaily(boolean resetDaily) {
        this.resetDaily = resetDaily;
    }
}
