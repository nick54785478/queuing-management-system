package com.example.qms.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "前端心跳續命請求載體")
public record PingTicketHeartbeatResource(
    @Schema(description = "等候憑證 (Queue Token)", example = "token-uuid")
    String queueToken,
    @Schema(description = "排隊號碼", example = "28")
    long ticketNumber
) {}
