package com.example.qms.application.command;

public record DequeueTicketCommand(
    String tenantId,
    String activityId,
    int limit,
    String idempotencyKey
) {}
