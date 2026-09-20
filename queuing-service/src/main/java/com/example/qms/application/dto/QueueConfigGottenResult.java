package com.example.qms.application.dto;

import com.example.qms.domain.config.aggregate.vo.QueueMode;

public record QueueConfigGottenResult(
    String tenantId,
    String activityId,
    int dequeueBatchSize,
    QueueMode mode,
    boolean resetDaily
) {}
