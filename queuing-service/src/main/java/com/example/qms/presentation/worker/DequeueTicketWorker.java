package com.example.qms.presentation.worker;

import com.example.qms.application.command.DequeueTicketCommand;
import com.example.qms.application.port.in.DequeueTicketUseCase;
import com.example.qms.application.port.in.GetQueueConfigUseCase;
import com.example.qms.application.query.GetQueueConfigQuery;
import com.example.qms.application.port.out.CapacityRepositoryPort;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.config.aggregate.vo.QueueMode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@EnableScheduling
public class DequeueTicketWorker {

    private final DequeueTicketUseCase dequeueTicketUseCase;
    private final GetQueueConfigUseCase getQueueConfigUseCase;
    private final RedissonClient redissonClient;
    private final CapacityRepositoryPort capacityRepositoryPort;

    public DequeueTicketWorker(DequeueTicketUseCase dequeueTicketUseCase, GetQueueConfigUseCase getQueueConfigUseCase, RedissonClient redissonClient, CapacityRepositoryPort capacityRepositoryPort) {
        this.dequeueTicketUseCase = dequeueTicketUseCase;
        this.getQueueConfigUseCase = getQueueConfigUseCase;
        this.redissonClient = redissonClient;
        this.capacityRepositoryPort = capacityRepositoryPort;
    }

    // Example scheduled task: fires every 5 seconds.
    // In a real SaaS, you would have a dynamic scheduler per tenant/activity,
    // or fetch active activities from a database to iterate over.
    // Here we hardcode an example tenant and activity for demonstration.
    @Scheduled(fixedRate = 5000)
    public void scheduledDequeue() {
        String tenantId = "demo-tenant";
        String activityId = "demo-activity";
        String lockKey = String.format("qms:lock:tenant:%s:activity:%s:dequeue", tenantId, activityId);

        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire the lock. Wait up to 1 second, lease for 4 seconds.
            // If another pod has the lock, this pod will just skip this run.
            boolean isLocked = lock.tryLock(1, 4, TimeUnit.SECONDS);
            if (isLocked) {
                log.debug("Acquired lock for {}, executing dequeue...", lockKey);
                
                com.example.qms.application.dto.QueueConfigGottenResult config = getQueueConfigUseCase.getQueueConfig(new GetQueueConfigQuery(tenantId, activityId)).join();
                
                if (config != null && config.mode() == QueueMode.MANUAL) {
                    log.debug("Manual mode enabled for {}, skipping auto dequeue", lockKey);
                    return;
                }
                
                int batchSize = config != null ? config.dequeueBatchSize() : 10;
                log.debug("Configured batch size: {}", batchSize);

                // Check capacity before dequeuing
                int availableToDequeue = capacityRepositoryPort.tryAcquireCapacity(new TenantId(tenantId), new ActivityId(activityId), batchSize).join();

                if (availableToDequeue <= 0) {
                    log.debug("No downstream capacity available for {}. Skipping auto dequeue.", lockKey);
                    return;
                }
                
                log.debug("Acquired {} tokens. Proceeding to dequeue...", availableToDequeue);

                // Pop up to 'availableToDequeue' tickets
                DequeueTicketCommand command = new DequeueTicketCommand(tenantId, activityId, availableToDequeue, java.util.UUID.randomUUID().toString());
                int dequeuedCount = dequeueTicketUseCase.dequeue(command).join(); // blocking in worker is okay for this simple example
                
                log.debug("Dequeue batch completed for {}. Requested: {}, Actually Dequeued: {}", lockKey, availableToDequeue, dequeuedCount);
                
                if (dequeuedCount < availableToDequeue) {
                    int unused = availableToDequeue - dequeuedCount;
                    capacityRepositoryPort.releaseCapacity(new TenantId(tenantId), new ActivityId(activityId), unused).join();
                    log.debug("Refunded {} unused capacity tokens for {}", unused, lockKey);
                }
            } else {
                log.trace("Failed to acquire lock for {}, another pod is likely executing it.", lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Dequeue worker thread interrupted", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
