package com.example.qms.application.query;

public record GetCapacityQuery(
        String tenantId,
        String activityId
) {
}
