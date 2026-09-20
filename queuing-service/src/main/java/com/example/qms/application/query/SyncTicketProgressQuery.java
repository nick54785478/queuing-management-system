package com.example.qms.application.query;

public record SyncTicketProgressQuery(
    String tenantId,
    String activityId
) {}
