package com.example.qms.application.command;

public record SetCapacityCommand(
        String tenantId,
        String activityId,
        int capacity
) {
}
