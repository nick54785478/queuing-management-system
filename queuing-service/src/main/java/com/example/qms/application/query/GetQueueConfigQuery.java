package com.example.qms.application.query;

public record GetQueueConfigQuery(
    String tenantId,
    String activityId
) {}
