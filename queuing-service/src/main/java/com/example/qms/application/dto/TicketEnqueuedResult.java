package com.example.qms.application.dto;

public record TicketEnqueuedResult(
    String ticketId,
    long ticketNumber,
    String queueToken,
    String status
) {}
