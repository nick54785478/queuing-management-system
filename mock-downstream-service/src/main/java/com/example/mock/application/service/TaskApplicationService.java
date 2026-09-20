package com.example.mock.application.service;

import com.example.mock.application.command.ProcessTaskCommand;
import com.example.mock.application.port.in.ProcessTaskUseCase;
import com.example.mock.application.port.out.TaskMessagePublisherPort;
import com.example.mock.domain.task.aggregate.vo.ActivityId;
import com.example.mock.domain.task.aggregate.vo.TenantId;
import com.example.mock.domain.task.aggregate.vo.TicketNumber;
import com.example.mock.domain.task.event.DownstreamTaskCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TaskApplicationService implements ProcessTaskUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(TaskApplicationService.class);
    
    private final TaskMessagePublisherPort messagePublisherPort;

    public TaskApplicationService(TaskMessagePublisherPort messagePublisherPort) {
        this.messagePublisherPort = messagePublisherPort;
    }

    @Override
    public CompletableFuture<Void> processTask(ProcessTaskCommand command) {
        TenantId tenantId = new TenantId(command.tenantId());
        ActivityId activityId = new ActivityId(command.activityId());
        TicketNumber ticketNumber = new TicketNumber(command.ticketNumber());

        log.info("User (ticket {}) entered system. Processing business logic for tenant {}, activity {}...", 
                ticketNumber.value(), tenantId.value(), activityId.value());

        // Simulate processing delay (e.g. 2 seconds to simulate a real business transaction)
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }).thenCompose(v -> {
            log.info("Business logic completed for ticket {}. Releasing capacity to QMS...", ticketNumber.value());
            DownstreamTaskCompletedEvent event = new DownstreamTaskCompletedEvent(
                    tenantId, activityId, ticketNumber, 1
            );
            return messagePublisherPort.publishTaskCompletedEvent(event);
        });
    }
}
