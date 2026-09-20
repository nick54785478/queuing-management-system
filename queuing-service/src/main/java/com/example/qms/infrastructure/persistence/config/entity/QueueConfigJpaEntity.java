package com.example.qms.infrastructure.persistence.config.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "queue_config", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "activity_id"})
})
public class QueueConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "activity_id", nullable = false)
    private String activityId;

    @Column(name = "dequeue_batch_size", nullable = false)
    private Integer dequeueBatchSize;

    @Column(name = "mode", nullable = false, columnDefinition = "varchar(255) default 'AUTOMATIC'")
    private String mode = "AUTOMATIC";

    @Column(name = "reset_daily", nullable = false, columnDefinition = "boolean default true")
    private Boolean resetDaily = true;

    // Constructors
    public QueueConfigJpaEntity() {}

    public QueueConfigJpaEntity(String tenantId, String activityId, Integer dequeueBatchSize, String mode, Boolean resetDaily) {
        this.tenantId = tenantId;
        this.activityId = activityId;
        this.dequeueBatchSize = dequeueBatchSize;
        this.mode = mode;
        this.resetDaily = resetDaily;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getDequeueBatchSize() {
        return dequeueBatchSize;
    }

    public void setDequeueBatchSize(Integer dequeueBatchSize) {
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
