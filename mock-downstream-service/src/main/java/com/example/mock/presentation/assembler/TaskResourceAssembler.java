package com.example.mock.presentation.assembler;

import com.example.mock.application.command.ProcessTaskCommand;
import com.example.mock.presentation.resource.in.ProcessTaskResource;
import org.springframework.stereotype.Component;

@Component
public class TaskResourceAssembler {

    public ProcessTaskCommand toCommand(ProcessTaskResource resource) {
        long ticketNumber = 0;
        if (resource.getTicketNumber() != null && !resource.getTicketNumber().isBlank()) {
            ticketNumber = Long.parseLong(resource.getTicketNumber());
        }
        
        return new ProcessTaskCommand(
                resource.getTenantId(),
                resource.getActivityId(),
                ticketNumber
        );
    }
}
