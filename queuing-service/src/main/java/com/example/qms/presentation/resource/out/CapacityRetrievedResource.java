package com.example.qms.presentation.resource.out;

public record CapacityRetrievedResource(
        int remainingCapacity,
        int maxCapacity
) {
}
