package com.example.qms.application.service;

import com.example.qms.application.command.UpdateQueueConfigCommand;
import com.example.qms.application.dto.QueueConfigGottenResult;
import com.example.qms.application.port.in.GetQueueConfigUseCase;
import com.example.qms.application.port.in.UpdateQueueConfigUseCase;
import com.example.qms.application.port.out.QueueConfigRepositoryPort;
import com.example.qms.application.query.GetQueueConfigQuery;
import com.example.qms.domain.config.aggregate.root.QueueConfig;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.concurrent.CompletableFuture;

/**
 * 佇列設定應用服務 (Application Service)
 * <p>
 * 負責處理與排隊活動設定 (QueueConfig) 相關的業務邏輯，包含建立、更新與查詢。
 * 協調領域模型 {@link QueueConfig} 與持久層介面。
 * </p>
 */
@Service
class QueueConfigApplicationService implements UpdateQueueConfigUseCase, GetQueueConfigUseCase {

    private final QueueConfigRepositoryPort queueConfigRepositoryPort;

    public QueueConfigApplicationService(QueueConfigRepositoryPort queueConfigRepositoryPort) {
        this.queueConfigRepositoryPort = queueConfigRepositoryPort;
    }

    /**
     * 更新或建立佇列設定
     * <p>
     * 若該租戶與活動的設定不存在，則會自動建立一筆新的設定。
     * </p>
     *
     * @param command 包含更新內容的命令物件 (例如模式、批次放行數量等)
     * @return CompletableFuture 包含更新後的設定結果
     */
    @Override
    public CompletableFuture<QueueConfigGottenResult> updateQueueConfig(UpdateQueueConfigCommand command) {
        return Mono.fromFuture(() -> queueConfigRepositoryPort.findByTenantIdAndActivityId(command.tenantId(), command.activityId()))
                .defaultIfEmpty(QueueConfig.create(command.tenantId(), command.activityId(), command.dequeueBatchSize(), command.mode(), command.resetDaily()))
                .flatMap(config -> {
                    config.updateDequeueBatchSize(command.dequeueBatchSize());
                    if (command.mode() != null) {
                        config.updateMode(command.mode());
                    }
                    config.updateResetDaily(command.resetDaily());
                    return Mono.fromFuture(() -> queueConfigRepositoryPort.save(config));
                })
                .map(saved -> new QueueConfigGottenResult(saved.getTenantId(), saved.getActivityId(), saved.getDequeueBatchSize(), saved.getMode(), saved.isResetDaily()))
                .toFuture();
    }

    /**
     * 查詢佇列設定
     *
     * @param query 包含欲查詢的租戶與活動識別
     * @return CompletableFuture 包含查詢到的設定結果
     */
    @Override
    public CompletableFuture<QueueConfigGottenResult> getQueueConfig(GetQueueConfigQuery query) {
        return Mono.fromFuture(() -> queueConfigRepositoryPort.findByTenantIdAndActivityId(query.tenantId(), query.activityId()))
                .map(config -> new QueueConfigGottenResult(config.getTenantId(), config.getActivityId(), config.getDequeueBatchSize(), config.getMode(), config.isResetDaily()))
                .toFuture();
    }
}
