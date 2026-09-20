package com.example.qms.application.command;

public record ReleaseCapacityCommand(
    String tenantId,
    String activityId,
    long ticketNumber,
    int releasedTokens
) {}
