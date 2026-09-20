package com.example.qms.presentation.resource.in;

public record ReleaseCapacityResource(
        String tenantId,
        String activityId,
        long ticketNumber,
        int completedCount
) {}
