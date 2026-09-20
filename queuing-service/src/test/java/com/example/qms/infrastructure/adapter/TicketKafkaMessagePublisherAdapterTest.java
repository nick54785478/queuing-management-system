package com.example.qms.infrastructure.adapter;

import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import com.example.qms.domain.ticket.aggregate.vo.TicketId;
import com.example.qms.domain.ticket.event.TicketDequeuedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketKafkaMessagePublisherAdapterTest {

    /**
     * Feature: Publish Ticket Dequeued Event
     * 
     * Scenario: Publishing a TicketDequeuedEvent successfully sends it to the correct multi-tenant Kafka topic
     *   Given a TicketDequeuedEvent with a specific TenantId "tenant-x" and TicketId "t-123"
     *   When the TicketKafkaAdapter publishes the event
     *   Then the KafkaTemplate should send the event to the topic "qms.tenant.tenant-x.ticket.dequeued" with the TicketId as the message key
     */
    @Test
    @SuppressWarnings("unchecked")
    void testPublishEventToCorrectTopic() {
        // Arrange
        KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        KafkaTicketMessagePublisherAdapter adapter = new KafkaTicketMessagePublisherAdapter(kafkaTemplate);

        TicketDequeuedEvent event = new TicketDequeuedEvent(
                new TicketId("t-123"),
                new TenantId("tenant-x"),
                new ActivityId("act-y"),
                10L,
                Instant.now()
        );

        when(kafkaTemplate.send(any(String.class), any(String.class), any(Object.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act
        adapter.publishTicketDequeuedEvent(event).join();

        // Verify the topic routing matches the TenantId
        verify(kafkaTemplate).send(
                eq("qms.tenant.tenant-x.ticket.dequeued"),
                eq("t-123"),
                eq(event)
        );
    }
}
