package com.example.qms.presentation.assembler;

import com.example.qms.application.command.EnqueueTicketCommand;
import com.example.qms.application.command.PingTicketHeartbeatCommand;
import com.example.qms.application.query.SyncTicketProgressQuery;
import com.example.qms.application.dto.TicketEnqueuedResult;
import com.example.qms.application.dto.TicketProgressGottenResult;
import com.example.qms.presentation.resource.in.EnqueueTicketResource;
import com.example.qms.presentation.resource.out.TicketEnqueuedResource;
import com.example.qms.presentation.resource.in.PingTicketHeartbeatResource;
import com.example.qms.presentation.resource.out.TicketProgressRetrievedResource;
import org.springframework.stereotype.Component;

@Component
public class TicketResourceAssembler {

    public EnqueueTicketCommand toCommand(String tenantId, String activityId, EnqueueTicketResource resource) {
        return new EnqueueTicketCommand(tenantId, activityId);
    }

    public TicketEnqueuedResource toResource(TicketEnqueuedResult result) {
        return new TicketEnqueuedResource(
                result.ticketId(),
                result.ticketNumber(),
                result.queueToken(),
                result.status()
        );
    }

    public SyncTicketProgressQuery toQuery(String tenantId, String activityId) {
        return new SyncTicketProgressQuery(tenantId, activityId);
    }

    public TicketProgressRetrievedResource toResource(TicketProgressGottenResult result) {
        return new TicketProgressRetrievedResource(result.currentServingNumber());
    }

    public PingTicketHeartbeatCommand toCommand(String tenantId, String activityId, PingTicketHeartbeatResource resource) {
        return new PingTicketHeartbeatCommand(tenantId, activityId, resource.queueToken(), resource.ticketNumber());
    }
}
