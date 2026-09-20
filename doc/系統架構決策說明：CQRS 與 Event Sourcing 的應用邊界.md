# 系統架構決策說明：CQRS 與 Event Sourcing 的應用邊界

在分散式高併發架構中，是否引入 **CQRS（命令查詢職責分離）** 與 **Event Sourcing（事件溯源，ES）**，核心的決策基準在於：**系統所處的「限界上下文（Bounded Context）」**。

總體原則：**強烈建議不要**將它們用在「排隊系統」本身，但**極度推薦**將它們應用在排隊放行後的「核心業務系統」（例如分散式火車訂票系統）。

以下為具體的架構拆解與評估建議：

---

## 1. 邊界層 (Queueing Layer)：為何不適合引入 CQRS 與 ES？

排隊系統的本質是**「高頻率、生命週期極短、無嚴格追溯價值」**的流量擋箭牌，應保持極簡。

*   **排隊不需要 Event Sourcing：**
    ES 的核心是記錄每一個狀態變更的歷史事件（例如：`UserEnqueued` $\rightarrow$ `UserMovedUp` $\rightarrow$ `UserDequeued`）。如果在海量併發的排隊層引入 ES，意味著每秒鐘要往 Event Store 寫入數十萬筆的排隊移動事件。這不僅會瞬間癱瘓資料庫，這些資料在幾分鐘後也將毫無價值（因為排隊順位的每一次推移不需要嚴格的重播或稽核）。
*   **排隊不需要 CQRS：** 
    排隊系統的 Read（查詢自己第幾名）與 Write（領號碼牌）在資料模型上極度一致，都是針對同一個 Redis Key（如 `ZSET`）進行操作，沒有將讀寫模型分離的必要。

---

## 2. 核心領域層 (Core Domain Layer)：為何極度推薦引入 CQRS？

當使用者通過排隊，進入到核心業務（如火車訂票、電商結帳）時，系統的特性就完全改變了。在這種複雜的領域模型中，**強烈建議引入 CQRS**，將複雜的查詢與業務規則分離。

以分散式火車訂票系統為例：
*   **讀取模型（Query - Read Model）：** 
    使用者會極度頻繁地查詢「班次時刻表」、「剩餘座位數」、「票價」。這些查詢需要極致的讀取效能，可以將資料預先聚合並存放於 Elasticsearch、Redis 或專門的 Read Database。
*   **寫入模型（Command - Write Model）：** 
    真正的「劃位」與「建立訂單」動作，頻率遠低於查詢，但需要極高的一致性與複雜的業務規則驗證。這部分由關聯式資料庫（RDBMS）的 Write Database 嚴格把關。
*   **架構價值：** 
    透過 Kafka 或 RabbitMQ 作為事件匯流排（Event Bus），當 Write Model 成功劃位後，發布 `SeatReservedEvent`，非同步地更新 Read Model 的剩餘座位數。這完美解耦了讀與寫的效能瓶頸。

---

## 3. 核心領域層引入 Event Sourcing (ES) 的權衡

在 CQRS 的基礎上，是否要進一步將 Write Model 升級為 Event Sourcing，需要權衡其巨大優勢與隱性成本。

### ES 的巨大優勢
*   **完美的稽核與除錯：** 傳統 RDBMS 只存最終狀態（例如 `status = 'CANCELLED'`）。ES 則記錄了完整的演進軌跡（`TicketBooked` $\rightarrow$ `PaymentFailed` $\rightarrow$ `TicketCancelled`）。對於訂票這類需要絕對防弊與事後追溯的系統，ES 提供了單一真相來源（Single Source of Truth）。
*   **天然契合微服務與響應式系統：** ES 產生的一連串事件，可以直接轉發到 Spring WebFlux 的響應式串流中，或作為 Saga 模式中協調跨服務交易（如通知付款服務、會員服務）的強大驅動來源。

### ES 的隱性成本
*   **開發心智負擔極大：** 開發人員不能再依賴簡單的 CRUD 與 `UPDATE` 語法，所有的狀態變更都必須抽象為不可變的事件（Immutable Events）。
*   **最終一致性（Eventual Consistency）的 UI 挑戰：** 當寫入事件到 Event Store，再非同步更新到 Read Model 時，必然存在短暫的時間差。前端需要特殊的設計（例如透過 WebSocket 接收更新通知，而不是寫入後立刻呼叫 API 重新查詢）來掩蓋這段時間差。

---

## 4. 架構配置總結與建議

1.  **排隊邊界層：** 保持極簡。只用 Redis `INCR` 與 `ZSET` 處理高頻狀態，**不引入** CQRS/ES。
2.  **核心訂票/交易層：**
    *   **預設採用 CQRS：** 將複雜的資源查詢與訂單建立分離，這在現代微服務架構中是處理高併發的基礎配置。
    *   **謹慎評估 ES：** 
        *   若系統高度要求「操作軌跡追溯」或「複雜的分散式狀態流轉」，才引入 ES。
        *   若團隊對 ES 不熟悉，強烈建議先採用 **「CQRS + Outbox Pattern」** 的組合方案。Outbox Pattern 能達到 Event Sourcing 80% 的核心好處（保證事件可靠遞送與微服務解耦），但實作複雜度與開發成本大幅降低。