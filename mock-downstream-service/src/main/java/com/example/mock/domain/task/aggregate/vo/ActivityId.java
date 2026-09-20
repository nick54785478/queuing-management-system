package com.example.mock.domain.task.aggregate.vo;

public record ActivityId(String value) {
    public ActivityId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ActivityId cannot be empty");
        }
    }
}
