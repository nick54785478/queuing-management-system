package com.example.qms.application.service;

import com.example.qms.application.command.EnqueueTicketCommand;
import com.example.qms.application.port.out.TicketRepositoryPort;
import com.example.qms.domain.ticket.aggregate.root.Ticket;
import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import com.example.qms.application.port.out.TicketMessagePublisherPort;
import com.example.qms.application.port.out.QueueConfigRepositoryPort;
import com.example.qms.application.port.out.IdempotencyHandlerPort;
import com.example.qms.domain.config.aggregate.root.QueueConfig;
import com.example.qms.domain.config.aggregate.vo.QueueMode;
import com.example.qms.application.dto.TicketEnqueuedResult;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class TicketApplicationServiceTest {

    @Test
    void shouldEnqueueTicketSuccessfully() {
        // Arrange
        TicketRepositoryPort repositoryPort = Mockito.mock(TicketRepositoryPort.class);
        TicketMessagePublisherPort publisherPort = Mockito.mock(TicketMessagePublisherPort.class);
        QueueConfigRepositoryPort configPort = Mockito.mock(QueueConfigRepositoryPort.class);
        IdempotencyHandlerPort idempotencyPort = Mockito.mock(IdempotencyHandlerPort.class);
        TicketApplicationService service = new TicketApplicationService(repositoryPort, publisherPort, configPort, idempotencyPort);

        EnqueueTicketCommand command = new EnqueueTicketCommand("tenant-a", "activity-x");
        Ticket mockTicket = new Ticket(new TenantId("tenant-a"), new ActivityId("activity-x"), 100L);
        
        QueueConfig mockConfig = QueueConfig.create("tenant-a", "activity-x", 100, QueueMode.AUTOMATIC, true);

        when(configPort.findByTenantIdAndActivityId(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockConfig));

        when(repositoryPort.enqueue(any(TenantId.class), any(ActivityId.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockTicket));

        // Act
        TicketEnqueuedResult result = service.enqueue(command).join();

        // Assert
        assert result.ticketNumber() == 100L;
        assert result.status().equals("QUEUING");
        assert result.ticketId() != null;
        assert result.queueToken() != null;
    }
}
