package com.example.qms.application.command;

import com.example.qms.domain.config.aggregate.vo.QueueMode;

public record UpdateQueueConfigCommand(
    String tenantId,
    String activityId,
    int dequeueBatchSize,
    QueueMode mode,
    boolean resetDaily
) {}
