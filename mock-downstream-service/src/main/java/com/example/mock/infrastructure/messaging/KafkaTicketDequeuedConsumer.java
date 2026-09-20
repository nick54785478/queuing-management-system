package com.example.mock.infrastructure.messaging;

import com.example.mock.presentation.resource.in.TicketDequeuedEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 【Infrastructure Layer】Kafka 訊息消費者 (Inbound Adapter/Driver)
 * 
 * 負責訂閱 QMS 發送的「號碼牌放行事件」(TicketDequeuedEvent)。
 * 在真實情境中，此消費者可能會觸發某些預熱邏輯或通知，但在目前的 Mock 服務中，
 * 它主要作為一個觀測點，用來在控制台打印日誌，證明下游系統已確實收到放行通知。
 */
@Component
public class KafkaTicketDequeuedConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaTicketDequeuedConsumer.class);

    /**
     * 監聽並處理 QMS 發送的號碼牌放行事件。
     * 支援萬用字元訂閱，能同時接收不同 tenant 與 activity 的放行事件。
     *
     * @param payload 來自 QMS 的放行事件純資料載體 (Data Payload)
     */
    @KafkaListener(topicPattern = "qms\\.tenant\\..*\\.ticket\\.dequeued", groupId = "mock-downstream-group")
    public void onTicketDequeued(TicketDequeuedEventPayload payload) {
        log.info("Mock Downstream: Received dequeued ticket #{} for tenant {}, activity {}", 
                payload.getTicketNumber(), 
                payload.getTenantId(), 
                payload.getActivityId());
    }
}
