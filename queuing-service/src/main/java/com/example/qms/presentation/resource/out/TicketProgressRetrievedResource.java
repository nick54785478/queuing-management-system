package com.example.qms.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "即時進度推播 (SSE) 結果載體")
public record TicketProgressRetrievedResource(
    @Schema(description = "當前已放行的最新號碼", example = "95")
    Long currentServingNumber
) {}
