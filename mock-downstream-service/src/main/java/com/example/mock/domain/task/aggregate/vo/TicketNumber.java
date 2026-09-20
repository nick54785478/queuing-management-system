package com.example.mock.domain.task.aggregate.vo;

public record TicketNumber(long value) {
    public TicketNumber {
        if (value <= 0) {
            throw new IllegalArgumentException("TicketNumber must be greater than 0");
        }
    }
}
