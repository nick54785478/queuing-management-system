# 企業級排隊系統 (Queueing Management System - QMS)

## 📖 專案概述

本專案實作一個高併發、高可用性的**排隊系統 (Queueing System)**。
它的核心定位是一個**「邊界防護盾 (Traffic Shield)」**，專門用來應對搶票、限量商品秒殺或突發性大規模行銷活動所帶來的瞬間海量流量。

透過本系統的**削峰填谷 (Peak Shaving)** 機制，我們可以在最前線將不受控的併發流量攔截下來，並快速配發號碼牌，再根據後端真實的處理量能，平穩、定速地將流量放行給後端核心業務服務（如訂單、劃位系統）。這確保了後端的關聯式資料庫不會因為瞬間的連線池耗盡或鎖競爭 (Lock Contention) 而崩潰（被流量沖倒）。

## 🌟 最新功能 (Recent Updates)

*   **動態排隊模式切換**：支援即時切換 **全自動放行 (AUTOMATIC)** 與 **手動放行 (MANUAL)** 模式，滿足電商搶購與門診叫號等不同情境。
*   **動態放行數量設定**：管理員可隨時調整單次批次放行的數量，靈活控制後端伺服器的壓力。
*   **每日自動歸零 (Daily Reset)**：內建強大的無排程 (Stateless) 日期字尾機制，過午夜後自動將號碼牌從 1 號開始重新發放，舊資料隨 Redis TTL 自動過期。
*   **高併發防護與冪等性 (Idempotency)**：放行機制透過 Redis Lua Script 確保絕對原子性 (Atomicity)，並在前、後端實作 UUID 冪等性防護，杜絕網路延遲導致的連點多扣號。
*   **視覺化前端介面**：使用 Angular 實作現代化 Glassmorphism (毛玻璃) 風格的「管理員儀表板」與「櫃台操作員介面」，支援 SSE 即時狀態推播。

## 🏛️ 核心架構與技術選型

系統嚴格將「高頻排隊狀態維護」與「核心業務處理」進行物理與邏輯上的解耦：

*   **Redis (高頻狀態管理與取號)**：系統的心臟。利用 Redis 的單執行緒特性，透過 `INCR` 產生嚴格遞增的號碼牌，並以 `ZSET` 實作絕對 FIFO 的排隊機制。
*   **Kafka / RabbitMQ (非同步削峰)**：放行後的請求會被封裝為事件（Event），保證後端微服務能以非同步、平穩的速率消化業務。
*   **Spring WebFlux + SSE (即時狀態推播)**：提供非阻塞的長連線，主動向前端推播目前叫號進度，讓等待過程完全透明化。
*   **RDBMS (核心防線)**：PostgreSQL / MySQL 退居幕後，保留昂貴的 ACID 特性，專注於防止超賣（樂觀鎖）與落實分散式交易。

## 💡 關鍵架構決策 (Architecture Decisions)

1.  **取號階段捨棄 RDBMS 的 ACID 保證**：排隊系統入口追求極致的吞吐量。我們放棄 RDBMS 的強一致性與行級鎖，改用 Redis 記憶體級別的原子操作 (Lua Script)。因為「號碼牌」是生命週期極短的過渡性資料，在極限流量下「讓系統活著」遠比「保證號碼絕對不遺失」更重要。
2.  **排隊層保持極簡，不使用 CQRS / ES**：排隊資料屬於高頻推移且無稽核價值的過渡狀態，引入事件溯源 (Event Sourcing) 或 CQRS 只會徒增複雜度並癱瘓系統。
3.  **核心領域層採用 CQRS + Outbox Pattern**：排隊放行後進入核心業務層，此時讀寫特徵改變。我們透過 CQRS 將複雜的查詢與業務分離，並透過 Outbox Pattern（取代開發成本極高的 Event Sourcing），保證事件跨微服務的可靠遞送。

## ⚖️ 容量控制與併發處理哲學 (Capacity vs. Concurrency)

排隊系統（基於容量與令牌桶設計）的根本目的 **並非「完全消滅併發」**，而是 **「將併發控制在下游系統可以安全承受的範圍內」**。

系統的「最大總容量 (Max Capacity)」決定了允許「同時」進入下游系統執行業務的人數：
*   **最大化吞吐量 (Capacity > 1)**：如果下游系統（例如 MySQL, 微服務）經過壓測能承受 100 個併發請求，應將最大容量設為 `100`。當 10,000 人同時湧入時，排隊系統會擋下 9,900 人，僅放行 100 人。這 100 人無論如何同時點擊執行，都不會超過下游極限。這能最大化系統處理效率，避免一進一出導致資源浪費。
*   **嚴格的序列化 (Capacity = 1)**：如果您的下游系統非常老舊，或者程式碼完全不具備 Thread-Safe（執行緒安全）的防禦機制，連 2 個併發都會導致資料庫死鎖或錯亂，才需要將最大容量嚴格設為 `1`。此時排隊系統會強制執行「一進一出」，前一個人未執行完畢（或斷線超時回收）之前，絕對不會放行下一個人。

## 🚀 快速啟動 (Quick Start)

專案內提供了完整的 Docker Compose 配置，一鍵啟動分散式基礎設施：

```bash
# 啟動包含 Postgres, Redis, KRaft 模式的 Kafka 與 Kafka UI
docker-compose up -d
```

### 基礎設施服務端點與 Port 號：
- **PostgreSQL**: `5432` (資料庫: `qms_db`)
- **Redis**: `6379` (密碼：`qms_redis_pass`)
- **Kafka (EXTERNAL)**: `9092`
- **Kafka UI**: `http://localhost:8090`

## 📚 系統文件

詳細的設計思路與需求規格，請參閱 `doc/` 目錄：
- 📄 [QMS 架構藍圖](doc/QMS%20架構藍圖.md)
- 📄 [QMS 軟體需求規格書 (SRS)](doc/QMS_軟體需求規格書_SRS.md)
- 📄 [取號不考慮 ACID 的原因](doc/取號不考慮%20ACID%20的原因%20.md)
- 📄 [CQRS 與 Event Sourcing 的應用邊界](doc/系統架構決策說明：CQRS%20與%20Event%20Sourcing%20的應用邊界.md)

## 🔗 下游系統整合指南 (Downstream Integration Guide)

為了保護下游核心業務系統（如訂單、劃位服務）不被瞬間流量擊垮，本排隊系統支援 **「容量感知放行機制 (Event-Driven Token Bucket)」**。在 `AUTOMATIC` (自動放行) 模式下，排隊系統將不再「無腦狂送」，而是會依據下游系統回報的處理進度，動態補充「放行令牌 (Tokens)」，有空位才放人。

### 如何與排隊系統整合？

下游微服務在處理完一筆或多筆業務邏輯後，只需要發送一個 **完成事件 (Task Completed Event)** 到 Kafka，排隊系統收到後就會立刻為您補充對應數量的放行額度。

*   **Kafka Topic**: `qms.downstream.task.completed`
*   **Message Format**: JSON
*   **Payload Schema**:
    ```json
    {
      "tenantId": "您的租戶ID (例如：demo-tenant)",
      "activityId": "您的活動ID (例如：demo-activity)",
      "completedCount": 1 // 本次消化掉的任務數量 (釋放的位子數)
    }
    ```

只要您的系統按照上述格式發送完成事件，排隊系統的 Worker 就會在下一次排程自動將下一個順位的使用者放行，真正實現基於您實際處理量能的「動態削峰填谷」！

> **⚠️ 極端高併發強烈建議 (Client-Side Batching)**
> 請下游系統實作「本地批次聚合」。也就是說，不要每處理完 1 筆訂單就送出 1 個 Kafka Event 或呼叫 1 次 API！
> 建議在記憶體中累積（例如：每 1 秒，或是每滿 100 筆），再發送單一事件並將 `completedCount` 設為 100。排隊系統的 Kafka Consumer 亦已經開啟 Batch Listener 進行聚合，雙層聚合能將 Redis 寫入壓力降低 99% 以上，達到百萬級的吞吐。

### 替代方案：HTTP API 整合 (無 EDA 環境適用)

如果您的下游系統較為傳統，無法發送 Kafka 訊息（不具備 EDA 能力），您也可以直接透過呼叫排隊系統的 HTTP API 來釋放額度：

*   **Endpoint**: `POST /api/v1/capacity/release`
*   **Content-Type**: `application/json`
*   **Payload Schema**:
    ```json
    {
      "tenantId": "您的租戶ID",
      "activityId": "您的活動ID",
      "completedCount": 1
    }
    ```
