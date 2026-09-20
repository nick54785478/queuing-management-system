package com.example.qms.infrastructure.adapter;

import com.example.qms.application.port.out.TicketMessagePublisherPort;
import com.example.qms.domain.ticket.event.TicketDequeuedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
class KafkaTicketMessagePublisherAdapter implements TicketMessagePublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    KafkaTicketMessagePublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CompletableFuture<Void> publishTicketDequeuedEvent(TicketDequeuedEvent event) {
        return CompletableFuture.runAsync(() -> {
            String topic = String.format("qms.tenant.%s.ticket.dequeued", event.tenantId().value());
            kafkaTemplate.send(topic, event.ticketId().value(), event);
        });
    }
}
