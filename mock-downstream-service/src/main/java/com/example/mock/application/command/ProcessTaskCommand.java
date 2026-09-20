package com.example.mock.application.command;

public record ProcessTaskCommand(
    String tenantId,
    String activityId,
    long ticketNumber
) {}
