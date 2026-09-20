package com.example.qms.application.port.out;

import com.example.qms.domain.ticket.event.TicketDequeuedEvent;
import java.util.concurrent.CompletableFuture;

/**
 * 【Outbound Port】號碼牌領域事件發布埠 (Message Publisher Port)
 * 
 * 此介面代表應用層向外部發布事件的合約。
 * 在此專案中，它主要被用來將已放行的號碼牌事件推播到 Message Broker (例如 Kafka)。
 * 確保下游系統 (如訂單系統、通知系統) 能夠對放行事件做出反應。
 */
public interface TicketMessagePublisherPort {

    /**
     * 發布號碼牌已被放行的領域事件。
     *
     * @param event 放行事件的實體，內含被放行的票號與相關資訊
     * @return CompletableFuture<Void> 表示發布完成
     */
    CompletableFuture<Void> publishTicketDequeuedEvent(TicketDequeuedEvent event);
}
