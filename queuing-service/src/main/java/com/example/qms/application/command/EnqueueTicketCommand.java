package com.example.qms.application.command;

public record EnqueueTicketCommand(
    String tenantId,
    String activityId
) {}
