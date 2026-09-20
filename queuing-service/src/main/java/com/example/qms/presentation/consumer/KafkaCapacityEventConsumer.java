package com.example.qms.presentation.consumer;

import com.example.qms.application.command.ReleaseCapacityCommand;
import com.example.qms.application.port.in.ReleaseCapacityUseCase;
import com.example.qms.presentation.resource.in.DownstreamTaskCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KafkaCapacityEventConsumer {

    private final ReleaseCapacityUseCase releaseCapacityUseCase;

    public KafkaCapacityEventConsumer(ReleaseCapacityUseCase releaseCapacityUseCase) {
        this.releaseCapacityUseCase = releaseCapacityUseCase;
    }

    /**
     * 監聽下游微服務發出的完成事件，以補充放行額度 (令牌)
     * 假設 topic 統一叫做 "qms.downstream.task.completed"
     */
    @KafkaListener(topics = "qms.downstream.task.completed", groupId = "qms-queuing-service-group")
    public void onDownstreamTaskCompleted(List<DownstreamTaskCompletedEvent> events) {
        if (events == null || events.isEmpty()) return;

        log.info("Received batch of {} task completed events from downstream.", events.size());

        for (DownstreamTaskCompletedEvent event : events) {
            String tenantId = event.tenantId();
            String activityId = event.activityId();
            long ticketNumber = event.ticketNumber();
            int count = event.completedCount();

            ReleaseCapacityCommand command = new ReleaseCapacityCommand(tenantId, activityId, ticketNumber, count);
            
            releaseCapacityUseCase.releaseCapacity(command)
                .thenAccept(v -> log.info("Successfully released {} capacities for tenant {}, activity {}, ticket {}", 
                        count, tenantId, activityId, ticketNumber))
                .exceptionally(ex -> {
                    log.error("Failed to release capacity for tenant {}, activity {}, ticket {}", tenantId, activityId, ticketNumber, ex);
                    return null;
                });
        }
    }
}
