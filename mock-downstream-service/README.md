# Mock Downstream Service

這是一個用於模擬企業級排隊系統 (QMS) **下游核心業務處理**的微服務。

在真實的高併發搶購或購票場景中，當使用者通過排隊系統的放行後，會進入到處理訂單、扣庫存或劃位的核心系統。為了測試排隊系統的完整運作流程（包含容量控制、放行機制與回呼釋放容量），我們建立了這個輕量級的 Mock 服務。

## 🎯 服務職責

1. **模擬業務處理**：提供 API 接收已放行的請求，並模擬一段業務處理時間（例如延遲 2 秒）。
2. **容量釋放通知**：在模擬業務處理完畢後，向 Kafka 發布 `DownstreamTaskCompletedEvent`（下游任務完成事件），通知 QMS 排隊系統該名使用者已處理完畢，系統可釋放容量並放行下一位排隊者。
3. **事件觀測**：透過 Kafka Consumer 訂閱 QMS 發布的 `TicketDequeuedEvent`（號碼牌放行事件），並在控制台列印日誌，用於觀測系統間的非同步事件傳遞是否正常。

## 🏗️ 系統架構 (Hexagonal Architecture)

為保持與主專案一致的架構標準，本 Mock 服務也嚴格遵循了 **Domain-Driven Design (DDD)** 與 **六角形架構 (Ports & Adapters)**。

*   **Domain Layer (`domain/task`)**：
    *   封裝 `TenantId`, `ActivityId`, `TicketNumber` 等 Value Objects，並負責基本的參數驗證。
    *   定義發送給 QMS 的核心領域事件：`DownstreamTaskCompletedEvent`。
*   **Application Layer (`application/`)**：
    *   **Inbound Port** (`ProcessTaskUseCase`)：定義接收外部請求的合約。
    *   **Outbound Port** (`TaskMessagePublisherPort`)：定義對外發送訊息的合約。
    *   **Service** (`TaskApplicationService`)：實作 UseCase，負責協調領域物件與模擬耗時業務，並在完成後呼叫 Outbound Port。
*   **Infrastructure Layer (`infrastructure/`)**：
    *   **Adapter** (`KafkaTaskMessagePublisherAdapter`)：實作 Outbound Port，負責將完成事件封裝並發送至 Kafka 的 `qms.downstream.task.completed` Topic。
    *   **Consumer** (`KafkaTicketDequeuedConsumer`)：直接訂閱 Kafka 的放行事件並記錄日誌。
*   **Presentation Layer (`presentation/`)**：
    *   **Controller** (`BusinessController`)：提供 HTTP POST 端點 `/api/mock/business/process` 供前端在接收到 SSE 放行通知後呼叫。
    *   **Assembler** (`TaskResourceAssembler`)：防腐層，將傳入的 JSON Payload 轉換為 Application Layer 所需的 `ProcessTaskCommand`。

## 🚀 啟動方式

本服務預設運行在 Port `8081`。

### 依賴環境
啟動前請確保本地端或 Docker 中的 Kafka (Port: `9092`) 正在運行。

### 透過 Maven 啟動
```bash
# 回到專案根目錄
cd ../

# 透過 Maven Wrapper 啟動 Mock 服務
./mvnw spring-boot:run -pl mock-downstream-service
```

## 🔄 互動流程 (與 QMS 的協作)

1. **使用者放行**：QMS 的背景 Worker 根據容量決定放行，發布 `TicketDequeuedEvent` 至 Kafka。
2. **事件觀測**：Mock 服務的 `KafkaTicketDequeuedConsumer` 收到事件，印出 Log。
3. **前端呼叫**：前端收到 WebFlux 的 SSE 通知，主動向 Mock 服務的 `BusinessController` 發起 HTTP 請求。
4. **模擬執行**：`TaskApplicationService` 暫停執行緒 2 秒，模擬訂單處理。
5. **釋放容量**：Mock 服務透過 `KafkaTaskMessagePublisherAdapter` 送出 `DownstreamTaskCompletedEvent` 到 Kafka。
6. **QMS 接收**：QMS 接收到完成事件，釋放對應的 Capacity，準備放行下一批使用者。
