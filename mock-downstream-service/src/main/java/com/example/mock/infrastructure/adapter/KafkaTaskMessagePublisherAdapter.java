package com.example.mock.infrastructure.adapter;

import com.example.mock.application.port.out.TaskMessagePublisherPort;
import com.example.mock.domain.task.event.DownstreamTaskCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class KafkaTaskMessagePublisherAdapter implements TaskMessagePublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaTaskMessagePublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CompletableFuture<Void> publishTaskCompletedEvent(DownstreamTaskCompletedEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", event.getTenantId().value());
        payload.put("activityId", event.getActivityId().value());
        payload.put("ticketNumber", event.getTicketNumber().value());
        payload.put("completedCount", event.getCompletedCount());

        return kafkaTemplate.send("qms.downstream.task.completed", payload).thenAccept(result -> {});
    }
}
