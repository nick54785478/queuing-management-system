package com.example.qms.domain.ticket.aggregate.vo;

public record CapacityState(
        int remainingCapacity,
        int maxCapacity
) {
}
