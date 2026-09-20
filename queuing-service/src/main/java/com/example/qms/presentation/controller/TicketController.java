package com.example.qms.presentation.controller;

import com.example.qms.application.port.in.EnqueueTicketUseCase;
import com.example.qms.application.port.in.PingTicketHeartbeatUseCase;
import com.example.qms.application.port.in.SyncTicketProgressUseCase;
import com.example.qms.presentation.assembler.TicketResourceAssembler;
import com.example.qms.presentation.resource.in.EnqueueTicketResource;
import com.example.qms.presentation.resource.out.TicketEnqueuedResource;
import com.example.qms.presentation.resource.in.PingTicketHeartbeatResource;
import com.example.qms.presentation.resource.out.TicketProgressRetrievedResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

@Tag(name = "排隊微服務 API", description = "提供 SaaS 多租戶的號碼牌發放、進度查詢與心跳續命功能")
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/activities/{activityId}/tickets")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class TicketController {

    private final EnqueueTicketUseCase enqueueTicketUseCase;
    private final SyncTicketProgressUseCase syncTicketProgressUseCase;
    private final PingTicketHeartbeatUseCase pingTicketHeartbeatUseCase;
    private final TicketResourceAssembler assembler;

    public TicketController(EnqueueTicketUseCase enqueueTicketUseCase, 
                            SyncTicketProgressUseCase syncTicketProgressUseCase,
                            PingTicketHeartbeatUseCase pingTicketHeartbeatUseCase,
                            TicketResourceAssembler assembler) {
        this.enqueueTicketUseCase = enqueueTicketUseCase;
        this.syncTicketProgressUseCase = syncTicketProgressUseCase;
        this.pingTicketHeartbeatUseCase = pingTicketHeartbeatUseCase;
        this.assembler = assembler;
    }

    @Operation(summary = "發放號碼牌 (Enqueue)", description = "使用者排隊取號，回傳當前取得的號碼與等候憑證 (Queue Token)")
    @PostMapping
    public CompletableFuture<TicketEnqueuedResource> enqueue(
            @Parameter(description = "租戶識別碼", example = "tenant-1") @PathVariable String tenantId,
            @Parameter(description = "活動識別碼", example = "act-100") @PathVariable String activityId,
            @RequestBody(required = false) EnqueueTicketResource resource) {
        
        var command = assembler.toCommand(tenantId, activityId, resource);
        return enqueueTicketUseCase.enqueue(command)
                .thenApply(assembler::toResource);
    }

    @Operation(summary = "即時進度推播 (SSE)", description = "建立 Server-Sent Events 長連線，每兩秒推播當前放行進度")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<TicketProgressRetrievedResource> streamProgress(
            @Parameter(description = "租戶識別碼", example = "tenant-1") @PathVariable String tenantId,
            @Parameter(description = "活動識別碼", example = "act-100") @PathVariable String activityId) {
        
        var query = assembler.toQuery(tenantId, activityId);
        return reactor.adapter.JdkFlowAdapter.flowPublisherToFlux(syncTicketProgressUseCase.syncProgress(query))
                .map(assembler::toResource);
    }

    @Operation(summary = "前端心跳續命 (Heartbeat)", description = "使用者排隊期間需定時發送心跳以維持 Queue Token 存活，防止斷線霸佔位置")
    @PutMapping("/heartbeat")
    public CompletableFuture<Void> heartbeat(
            @Parameter(description = "租戶識別碼", example = "tenant-1") @PathVariable String tenantId,
            @Parameter(description = "活動識別碼", example = "act-100") @PathVariable String activityId,
            @RequestBody PingTicketHeartbeatResource resource) {
        
        var command = assembler.toCommand(tenantId, activityId, resource);
        return pingTicketHeartbeatUseCase.ping(command);
    }
}
