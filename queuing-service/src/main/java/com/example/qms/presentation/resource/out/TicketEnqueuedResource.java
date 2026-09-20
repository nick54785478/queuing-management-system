package com.example.qms.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 【Resource】獲取號碼牌結果資源
 * 
 * 代表排隊者在成功領取號碼牌後，收到的結果載體。
 * 內含識別資訊與狀態，供後續顯示排隊進度使用。
 */
@Schema(description = "取號成功後的回傳結果載體")
public record TicketEnqueuedResource(
    @Schema(description = "號碼牌全球唯一 ID", example = "ticket-uuid")
    String ticketId,
    @Schema(description = "您的等候號碼", example = "105")
    Long ticketNumber,
    @Schema(description = "您的等候憑證 (Queue Token，用於心跳續命)", example = "token-uuid")
    String queueToken,
    @Schema(description = "目前排隊狀態", example = "QUEUING")
    String status
) {}
