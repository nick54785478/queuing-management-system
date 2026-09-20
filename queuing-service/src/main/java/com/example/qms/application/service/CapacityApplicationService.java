package com.example.qms.application.service;

import com.example.qms.application.command.PublishReleaseCapacityEventCommand;
import com.example.qms.application.command.ReleaseCapacityCommand;
import com.example.qms.application.port.in.PublishReleaseCapacityEventUseCase;
import com.example.qms.application.port.in.ReleaseCapacityUseCase;
import com.example.qms.application.port.in.GetCapacityUseCase;
import com.example.qms.application.port.in.SetCapacityUseCase;
import com.example.qms.application.query.GetCapacityQuery;
import com.example.qms.application.command.SetCapacityCommand;
import com.example.qms.application.port.out.CapacityEventPublisherPort;
import com.example.qms.application.port.out.CapacityRepositoryPort;
import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import com.example.qms.domain.ticket.aggregate.vo.CapacityState;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 容量與放行額度應用服務 (Application Service)
 * <p>
 * 負責協調領域模型與外部基礎設施，提供容量釋放 (Token Bucket) 的使用案例實作。
 * 包含直接釋放額度至 Redis 以及透過 Kafka 非同步發布事件兩種方式。
 * </p>
 */
@Service
class CapacityApplicationService implements ReleaseCapacityUseCase, PublishReleaseCapacityEventUseCase, GetCapacityUseCase, SetCapacityUseCase {

    private final CapacityRepositoryPort capacityRepositoryPort;
    private final CapacityEventPublisherPort capacityEventPublisherPort;
    private final com.example.qms.application.port.out.QueueConfigRepositoryPort queueConfigRepositoryPort;

    public CapacityApplicationService(CapacityRepositoryPort capacityRepositoryPort, CapacityEventPublisherPort capacityEventPublisherPort, com.example.qms.application.port.out.QueueConfigRepositoryPort queueConfigRepositoryPort) {
        this.capacityRepositoryPort = capacityRepositoryPort;
        this.capacityEventPublisherPort = capacityEventPublisherPort;
        this.queueConfigRepositoryPort = queueConfigRepositoryPort;
    }

    private java.util.concurrent.CompletableFuture<String> getDateSuffix(String tenantId, String activityId) {
        return queueConfigRepositoryPort.findByTenantIdAndActivityId(tenantId, activityId)
                .thenApply(config -> config.isResetDaily() ? java.time.LocalDate.now().toString() : "");
    }

    /**
     * 直接釋放容量額度至後端儲存庫 (如 Redis)
     * 
     * @param command 包含租戶、活動及欲釋放數量之命令
     * @return CompletableFuture 代表非同步執行結果
     */
    @Override
    public CompletableFuture<Void> releaseCapacity(ReleaseCapacityCommand command) {
        TenantId tenantId = new TenantId(command.tenantId());
        ActivityId activityId = new ActivityId(command.activityId());
        
        return getDateSuffix(command.tenantId(), command.activityId())
                .thenCompose(dateSuffix -> capacityRepositoryPort.removeActiveSessionAndRelease(tenantId, activityId, command.ticketNumber(), command.releasedTokens(), dateSuffix))
                .thenAccept(released -> {
                    if (Boolean.FALSE.equals(released)) {
                        // Log or ignore if the session was already reclaimed by timeout
                    }
                });
    }

    /**
     * 將釋放額度的請求發布至 Message Broker (如 Kafka)
     * 供不具備 EDA 的下游系統透過 HTTP API 呼叫，轉為非同步事件處理
     * 
     * @param command 包含租戶、活動及欲釋放數量之命令
     * @return CompletableFuture 代表非同步執行結果
     */
    @Override
    public CompletableFuture<Void> publish(PublishReleaseCapacityEventCommand command) {
        return capacityEventPublisherPort.publishTaskCompletedEvent(
                command.tenantId(), 
                command.activityId(), 
                command.ticketNumber(),
                command.completedCount()
        );
    }

    @Override
    public CompletableFuture<CapacityState> getCapacity(GetCapacityQuery query) {
        TenantId tenantId = new TenantId(query.tenantId());
        ActivityId activityId = new ActivityId(query.activityId());
        return capacityRepositoryPort.getCapacity(tenantId, activityId);
    }

    @Override
    public CompletableFuture<Void> setCapacity(SetCapacityCommand command) {
        TenantId tenantId = new TenantId(command.tenantId());
        ActivityId activityId = new ActivityId(command.activityId());
        return capacityRepositoryPort.setCapacity(tenantId, activityId, command.capacity());
    }
}
