package com.example.qms.presentation.controller;

import com.example.qms.application.command.UpdateQueueConfigCommand;
import com.example.qms.application.port.in.GetQueueConfigUseCase;
import com.example.qms.application.port.in.UpdateQueueConfigUseCase;
import com.example.qms.application.query.GetQueueConfigQuery;
import com.example.qms.domain.config.aggregate.vo.QueueMode;
import com.example.qms.presentation.assembler.QueueConfigResourceAssembler;
import com.example.qms.presentation.resource.in.UpdateQueueConfigResource;
import com.example.qms.presentation.resource.out.QueueConfigRetrievedResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/admin/tenants/{tenantId}/activities/{activityId}")
@Tag(name = "Admin Config", description = "管理者專用：排隊系統動態設定 API")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AdminQueueConfigController {

    private final UpdateQueueConfigUseCase updateQueueConfigUseCase;
    private final GetQueueConfigUseCase getQueueConfigUseCase;
    private final QueueConfigResourceAssembler assembler;

    public AdminQueueConfigController(UpdateQueueConfigUseCase updateQueueConfigUseCase,
                                      GetQueueConfigUseCase getQueueConfigUseCase,
                                      QueueConfigResourceAssembler assembler) {
        this.updateQueueConfigUseCase = updateQueueConfigUseCase;
        this.getQueueConfigUseCase = getQueueConfigUseCase;
        this.assembler = assembler;
    }

    @PutMapping("/config")
    @Operation(summary = "更新活動排隊設定", description = "動態更新排隊放行數量，會同步寫入 PostgreSQL 與 Redis")
    public CompletableFuture<ResponseEntity<QueueConfigRetrievedResource>> updateConfig(
            @PathVariable String tenantId,
            @PathVariable String activityId,
            @RequestBody UpdateQueueConfigResource request) {

        QueueMode parsedMode = null;
        if (request.getMode() != null) {
            try {
                parsedMode = QueueMode.valueOf(request.getMode().toUpperCase());
            } catch (IllegalArgumentException e) {
                return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());
            }
        }

        UpdateQueueConfigCommand command = new UpdateQueueConfigCommand(
                tenantId,
                activityId,
                request.getDequeueBatchSize(),
                parsedMode,
                request.isResetDaily()
        );

        return updateQueueConfigUseCase.updateQueueConfig(command)
                .thenApply(assembler::toResource)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/config")
    @Operation(summary = "取得活動排隊設定", description = "從 Redis 快取或資料庫中取得目前的放行設定")
    public CompletableFuture<ResponseEntity<QueueConfigRetrievedResource>> getConfig(
            @PathVariable String tenantId,
            @PathVariable String activityId) {

        GetQueueConfigQuery query = new GetQueueConfigQuery(tenantId, activityId);

        return getQueueConfigUseCase.getQueueConfig(query)
                .thenApply(result -> {
                    if (result == null) {
                        return ResponseEntity.notFound().build();
                    }
                    return ResponseEntity.ok(assembler.toResource(result));
                });
    }
}
