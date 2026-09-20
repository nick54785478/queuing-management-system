package com.example.qms.infrastructure.adapter;

import com.example.qms.application.port.out.CapacityEventPublisherPort;
import com.example.qms.presentation.resource.in.DownstreamTaskCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class KafkaCapacityEventPublisherAdapter implements CapacityEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaCapacityEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CompletableFuture<Void> publishTaskCompletedEvent(String tenantId, String activityId, long ticketNumber, int completedCount) {
        DownstreamTaskCompletedEvent event = new DownstreamTaskCompletedEvent(tenantId, activityId, ticketNumber, completedCount);
        return kafkaTemplate.send("qms.downstream.task.completed", event)
                .thenApply(result -> null);
    }
}
