package com.example.qms.application.command;

public record PublishReleaseCapacityEventCommand(
        String tenantId,
        String activityId,
        long ticketNumber,
        int completedCount
) {}
