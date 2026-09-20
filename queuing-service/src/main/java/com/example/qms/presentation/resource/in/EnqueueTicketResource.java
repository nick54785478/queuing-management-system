package com.example.qms.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 【Resource】取號請求資源
 * 
 * 代表排隊者在發送領取號碼牌請求時，POST 請求的 Body 格式。
 * 此資源僅承載前端需要的擴充資料 (如備註)，不包含識別資訊 (ID 來自 Path)。
 */
@Schema(description = "取號請求載體 (目前保留為空，供未來擴充)")
public record EnqueueTicketResource() {
    // Body can be empty for now if no extra data is needed, since TenantId and ActivityId come from path
}
