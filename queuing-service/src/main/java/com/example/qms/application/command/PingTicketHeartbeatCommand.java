package com.example.qms.application.command;

public record PingTicketHeartbeatCommand(
    String tenantId,
    String activityId,
    String queueToken,
    long ticketNumber
) {}
