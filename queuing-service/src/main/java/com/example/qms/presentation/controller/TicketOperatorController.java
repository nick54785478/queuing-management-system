package com.example.qms.presentation.controller;

import com.example.qms.application.command.DequeueTicketCommand;
import com.example.qms.application.dto.QueueConfigGottenResult;
import com.example.qms.application.port.in.DequeueTicketUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.concurrent.CompletableFuture;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import java.time.Duration;
import com.example.qms.application.port.in.GetQueueConfigUseCase;
import com.example.qms.application.query.GetQueueConfigQuery;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/activities/{activityId}/tickets")
@Tag(name = "Ticket Operator", description = "操作員/中立放行操作 API")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class TicketOperatorController {

    private final DequeueTicketUseCase dequeueTicketUseCase;
    private final GetQueueConfigUseCase getQueueConfigUseCase;

    public TicketOperatorController(DequeueTicketUseCase dequeueTicketUseCase, 
                                    GetQueueConfigUseCase getQueueConfigUseCase) {
        this.dequeueTicketUseCase = dequeueTicketUseCase;
        this.getQueueConfigUseCase = getQueueConfigUseCase;
    }

    @PostMapping("/dequeue")
    @Operation(summary = "手動放行號碼", description = "供任何中立人員(櫃台、小編、醫生)手動呼叫下一位")
    public Mono<ResponseEntity<Void>> manualDequeue(
            @PathVariable String tenantId,
            @PathVariable String activityId,
            @RequestParam(required = false) Integer limit,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
        }

        Mono<Integer> limitMono = limit != null ? Mono.just(limit) : 
            Mono.fromFuture(getQueueConfigUseCase.getQueueConfig(new GetQueueConfigQuery(tenantId, activityId)))
                .map(QueueConfigGottenResult::dequeueBatchSize);

        return limitMono.flatMap(actualLimit -> {
            DequeueTicketCommand command = new DequeueTicketCommand(tenantId, activityId, actualLimit, idempotencyKey);
            return Mono.fromFuture(dequeueTicketUseCase.dequeue(command))
                    .map(v -> ResponseEntity.ok().<Void>build());
        });
    }
}
