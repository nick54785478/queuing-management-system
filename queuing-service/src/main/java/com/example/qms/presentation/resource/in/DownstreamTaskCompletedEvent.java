package com.example.qms.presentation.resource.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DownstreamTaskCompletedEvent(
    String tenantId,
    String activityId,
    long ticketNumber,
    int completedCount
) {}
