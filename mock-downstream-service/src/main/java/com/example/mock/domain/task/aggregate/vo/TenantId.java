package com.example.mock.domain.task.aggregate.vo;

public record TenantId(String value) {
    public TenantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be empty");
        }
    }
}
