package com.example.qms.presentation.assembler;

import com.example.qms.application.command.PublishReleaseCapacityEventCommand;
import com.example.qms.presentation.resource.in.ReleaseCapacityResource;
import org.springframework.stereotype.Component;

@Component
public class CapacityResourceAssembler {

    public PublishReleaseCapacityEventCommand toCommand(ReleaseCapacityResource resource) {
        return new PublishReleaseCapacityEventCommand(
                resource.tenantId(),
                resource.activityId(),
                resource.ticketNumber(),
                resource.completedCount()
        );
    }
}
