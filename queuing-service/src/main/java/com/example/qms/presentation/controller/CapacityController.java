package com.example.qms.presentation.controller;

import com.example.qms.application.port.in.PublishReleaseCapacityEventUseCase;
import com.example.qms.presentation.assembler.CapacityResourceAssembler;
import com.example.qms.presentation.resource.in.ReleaseCapacityResource;
import com.example.qms.presentation.resource.in.ReleaseCapacityResource;
import com.example.qms.presentation.resource.in.SetCapacityResource;
import com.example.qms.presentation.resource.out.CapacityRetrievedResource;
import com.example.qms.application.port.in.GetCapacityUseCase;
import com.example.qms.application.port.in.SetCapacityUseCase;
import com.example.qms.application.query.GetCapacityQuery;
import com.example.qms.application.command.SetCapacityCommand;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import io.swagger.v3.oas.annotations.Operation;

import java.util.concurrent.CompletableFuture;

@Tag(name = "Capacity", description = "容量與放行額度管理 API")
@RestController
@RequestMapping("/api/v1/capacity")
@CrossOrigin(originPatterns = "*", allowCredentials = "true") // 允許跨域請求與憑證
public class CapacityController {

    private final PublishReleaseCapacityEventUseCase publishReleaseCapacityEventUseCase;
    private final CapacityResourceAssembler capacityResourceAssembler;
    private final GetCapacityUseCase getCapacityUseCase;
    private final SetCapacityUseCase setCapacityUseCase;

    public CapacityController(PublishReleaseCapacityEventUseCase publishReleaseCapacityEventUseCase, 
                              CapacityResourceAssembler capacityResourceAssembler,
                              GetCapacityUseCase getCapacityUseCase,
                              SetCapacityUseCase setCapacityUseCase) {
        this.publishReleaseCapacityEventUseCase = publishReleaseCapacityEventUseCase;
        this.capacityResourceAssembler = capacityResourceAssembler;
        this.getCapacityUseCase = getCapacityUseCase;
        this.setCapacityUseCase = setCapacityUseCase;
    }

    @Operation(summary = "釋放容量", description = "供不具備 EDA 能力的下游系統透過 HTTP API 主動釋放處理額度")
    @PostMapping("/release")
    public CompletableFuture<ResponseEntity<Void>> releaseCapacity(@RequestBody ReleaseCapacityResource resource) {
        var command = capacityResourceAssembler.toCommand(resource);
        
        return publishReleaseCapacityEventUseCase.publish(command)
                .thenApply(v -> ResponseEntity.ok().<Void>build());
    }

    @Operation(summary = "取得目前容量", description = "取得下游系統目前的剩餘容量")
    @GetMapping("/tenants/{tenantId}/activities/{activityId}")
    public CompletableFuture<ResponseEntity<CapacityRetrievedResource>> getCapacity(
            @PathVariable String tenantId,
            @PathVariable String activityId) {
        return getCapacityUseCase.getCapacity(new GetCapacityQuery(tenantId, activityId))
                .thenApply(capacityState -> ResponseEntity.ok(new CapacityRetrievedResource(capacityState.remainingCapacity(), capacityState.maxCapacity())));
    }

    @Operation(summary = "重置/覆蓋容量", description = "強制覆蓋下游系統目前的總可用容量 (用於管理員操作)")
    @PostMapping("/tenants/{tenantId}/activities/{activityId}/reset")
    public CompletableFuture<ResponseEntity<Void>> setCapacity(
            @PathVariable String tenantId,
            @PathVariable String activityId,
            @RequestBody SetCapacityResource resource) {
        return setCapacityUseCase.setCapacity(new SetCapacityCommand(tenantId, activityId, resource.capacity()))
                .thenApply(v -> ResponseEntity.ok().<Void>build());
    }
}
