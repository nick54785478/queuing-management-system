package com.example.qms.application.service;

import com.example.qms.application.command.DequeueTicketCommand;
import com.example.qms.application.command.EnqueueTicketCommand;
import com.example.qms.application.command.PingTicketHeartbeatCommand;
import com.example.qms.application.query.SyncTicketProgressQuery;
import com.example.qms.application.dto.TicketEnqueuedResult;
import com.example.qms.application.dto.TicketProgressGottenResult;
import com.example.qms.application.port.in.DequeueTicketUseCase;
import com.example.qms.application.port.in.EnqueueTicketUseCase;
import com.example.qms.application.port.in.PingTicketHeartbeatUseCase;
import com.example.qms.application.port.in.SyncTicketProgressUseCase;
import com.example.qms.application.port.out.TicketMessagePublisherPort;
import com.example.qms.application.port.out.TicketRepositoryPort;
import com.example.qms.application.port.out.QueueConfigRepositoryPort;
import com.example.qms.application.port.out.IdempotencyHandlerPort;
import java.time.LocalDate;
import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import com.example.qms.domain.ticket.event.TicketDequeuedEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.concurrent.CompletableFuture;

import java.util.concurrent.Flow;
import java.time.Duration;

/**
 * 號碼牌核心應用服務 (Application Service)
 * <p>
 * 負責處理排隊核心業務邏輯，包含取號 (Enqueue)、放行 (Dequeue)、進度查詢 (SyncProgress)
 * 以及心跳續命 (Ping)。為確保在極端併發下的高吞吐量，主要操作皆會委派給底層
 * 儲存庫 (如 Redis) 以非同步方式執行。
 * </p>
 */
@Service
class TicketApplicationService implements 
        EnqueueTicketUseCase, 
        DequeueTicketUseCase, 
        SyncTicketProgressUseCase, 
        PingTicketHeartbeatUseCase {

    private final TicketRepositoryPort ticketRepositoryPort;
    private final TicketMessagePublisherPort ticketMessagePublisherPort;
    private final QueueConfigRepositoryPort queueConfigRepositoryPort;
    private final IdempotencyHandlerPort idempotencyPort;

    TicketApplicationService(TicketRepositoryPort ticketRepositoryPort, TicketMessagePublisherPort ticketMessagePublisherPort, QueueConfigRepositoryPort queueConfigRepositoryPort, IdempotencyHandlerPort idempotencyPort) {
        this.ticketRepositoryPort = ticketRepositoryPort;
        this.ticketMessagePublisherPort = ticketMessagePublisherPort;
        this.queueConfigRepositoryPort = queueConfigRepositoryPort;
        this.idempotencyPort = idempotencyPort;
    }

    private Mono<String> getDateSuffix(String tenantId, String activityId) {
        return Mono.fromFuture(() -> queueConfigRepositoryPort.findByTenantIdAndActivityId(tenantId, activityId))
                .map(config -> config.isResetDaily() ? LocalDate.now().toString() : "")
                .defaultIfEmpty(LocalDate.now().toString());
    }

    /**
     * 取號 (發配號碼牌)
     * <p>
     * 使用者請求進入排隊隊列。系統會依據當天的日期後綴 (若啟用每日重置) 
     * 在 Redis 進行嚴格遞增發號，並保證回傳一組唯一的 Queue Token。
     * </p>
     *
     * @param command 包含租戶與活動識別
     * @return CompletableFuture 包含發配的號碼牌詳細資訊
     */
    @Override
    public CompletableFuture<TicketEnqueuedResult> enqueue(EnqueueTicketCommand command) {
        TenantId tenantId = new TenantId(command.tenantId());
        ActivityId activityId = new ActivityId(command.activityId());

        return getDateSuffix(command.tenantId(), command.activityId())
                .flatMap(dateSuffix -> Mono.fromFuture(() -> ticketRepositoryPort.enqueue(tenantId, activityId, dateSuffix)))
                .map(ticket -> new TicketEnqueuedResult(
                        ticket.getTicketId().value(),
                        ticket.getTicketNumber(),
                        ticket.getQueueToken().value(),
                        ticket.getStatus().name()
                ))
                .toFuture();
    }

    /**
     * 放行號碼牌 (從排隊轉為可進入下游業務)
     * <p>
     * 支援冪等性防護，避免同一批次重複扣號。
     * 成功放行後，會發布 TicketDequeuedEvent 事件，讓下游 (如 Frontend SSE) 
     * 能夠收到最新進度的推播。
     * </p>
     *
     * @param command 包含要放行的數量限制 (limit) 與冪等鍵
     * @return CompletableFuture 回傳實際成功放行的人數
     */
    @Override
    public CompletableFuture<Integer> dequeue(DequeueTicketCommand command) {
        TenantId tenantId = new TenantId(command.tenantId());
        ActivityId activityId = new ActivityId(command.activityId());

        return idempotencyPort.checkIdempotency(command.idempotencyKey(), Duration.ofMinutes(5))
                .thenCompose(isNew -> {
                    if (Boolean.FALSE.equals(isNew)) {
                        // 如果已經處理過，視為成功但不做任何事 (冪等)
                        return CompletableFuture.completedFuture(0);
                    }
                    return getDateSuffix(command.tenantId(), command.activityId())
                            .flatMapMany(dateSuffix -> reactor.adapter.JdkFlowAdapter.flowPublisherToFlux(ticketRepositoryPort.dequeue(tenantId, activityId, command.limit(), dateSuffix)))
                            .flatMap(ticket -> {
                    ticket.dequeue();
                    
                    var events = ticket.getDomainEvents();
                    
                    return Flux.fromIterable(events)
                            .filter(e -> e instanceof TicketDequeuedEvent)
                            .cast(TicketDequeuedEvent.class)
                            .flatMap(e -> Mono.fromFuture(() -> ticketMessagePublisherPort.publishTicketDequeuedEvent(e))
                                    .thenReturn(ticket)); // Return the ticket so count() works
                    })
                    .count()
                    .map(Long::intValue)
                    .toFuture();
                });
    }

    /**
     * 同步當前叫號進度
     * <p>
     * 提供給前端使用的非同步事件流 (Reactive Stream)，每 2 秒輪詢一次當前最新的
     * 放行號碼。配合 WebFlux 可實作 Server-Sent Events (SSE)。
     * </p>
     *
     * @param query 查詢條件
     * @return Flow.Publisher 包含持續推送的進度結果
     */
    @Override
    public Flow.Publisher<TicketProgressGottenResult> syncProgress(SyncTicketProgressQuery query) {
        TenantId tenantId = new TenantId(query.tenantId());
        ActivityId activityId = new ActivityId(query.activityId());

        Flux<TicketProgressGottenResult> flux = Flux.interval(Duration.ofSeconds(2))
                .flatMap(tick -> getDateSuffix(query.tenantId(), query.activityId())
                        .flatMap(dateSuffix -> Mono.fromFuture(() -> ticketRepositoryPort.getCurrentServingNumber(tenantId, activityId, dateSuffix))))
                .map(TicketProgressGottenResult::new);

        return reactor.adapter.JdkFlowAdapter.publisherToFlowPublisher(flux);
    }

    /**
     * 延長號碼牌的生命週期 (心跳續命)
     * <p>
     * 避免使用者在等待期間號碼牌過期被回收，前端需定期呼叫此方法。
     * </p>
     *
     * @param command 包含要續命的 Queue Token
     * @return CompletableFuture
     */
    @Override
    public CompletableFuture<Void> ping(PingTicketHeartbeatCommand command) {
        TenantId tenantId = new TenantId(command.tenantId());
        ActivityId activityId = new ActivityId(command.activityId());
        return getDateSuffix(command.tenantId(), command.activityId())
                .flatMap(dateSuffix -> Mono.fromFuture(() -> ticketRepositoryPort.updateActiveSession(tenantId, activityId, command.queueToken(), command.ticketNumber(), dateSuffix)))
                .then().toFuture();
    }
}
