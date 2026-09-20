# 企業級排隊系統 (Queueing System) 軟體需求規格書 (SRS)

## 1. 簡介 (Introduction)

### 1.1 目的 (Purpose)
本文件旨在定義「企業級排隊系統（Queueing Management System, QMS）」的架構設計、系統行為與軟體需求。此文件將作為開發、測試與維護團隊的技術依據，確保系統能滿足高併發場景下的效能與穩定性要求。

### 1.2 系統範圍 (Scope)
本系統主要作為高流量業務（如搶票、限量商品發售等）的邊界防護層。核心目標是**保護後端核心業務不被瞬間海量流量擊垮**，透過削峰填谷（Peak Shaving）機制，確保後端微服務能以平穩速率處理請求，同時提供前端使用者公平、透明的等待體驗。

---

## 2. 系統架構與技術選型 (System Architecture)

系統嚴格將「高頻排隊狀態」與「核心業務處理」解耦，主要技術選型如下：
- **Redis**：系統心臟。利用 `INCR` 產生遞增號碼，並以 `ZSET` 實作絕對 FIFO 的排隊機制，負責扛住極高頻率的狀態變更。
- **Kafka / RabbitMQ**：訊息中介層，負責將放行後的請求封裝成事件（Event），提供非同步削峰與微服務解耦。
- **Spring WebFlux**：排隊伺服器。利用響應式非阻塞架構建立 Server-Sent Events (SSE) 長連線，向前端推播即時叫號進度。
- **RDBMS (PostgreSQL/MySQL)**：退居幕後，僅負責核心業務的 ACID 保證、防超賣樂觀鎖（Optimistic Locking）及落實分散式交易的 Outbox Pattern。
- **API Gateway / BFF**：邊界防禦層，負責阻擋黃牛與惡意機器人。

---

## 3. 功能性需求 (Functional Requirements)

### 3.1 獲取號碼牌 (Enqueue)
- **FR-1.1 防護與限流**：系統入口需透過 API Gateway 進行 CAPTCHA 驗證、動態請求簽章驗證及 IP 頻率限制，防堵腳本與機器人。
- **FR-1.2 原子性取號**：透過 Redis Lua Script 將「檢查總量上限」與「產生號碼 (`INCR`)」打包為原子操作，保證號碼牌的全域唯一性且無 Race Condition。
- **FR-1.3 排隊寫入**：將產生的號碼作為 Score，使用者 ID 作為 Member 寫入 Redis `ZSET`。
- **FR-1.4 憑證配發**：取號成功後，系統須配發專屬的 Queue Token 及排隊號碼（Ticket Number）給前端。

### 3.2 即時狀態更新 (Waiting & SSE Pushing)
- **FR-2.1 連線建立**：前端攜帶 Queue Token 透過 `EventSource` 與 WebFlux 建立 SSE 連線。
- **FR-2.2 進度廣播**：系統需每秒查詢 Redis 中的「目前已放行最大號碼」，並主動推播給所有 SSE 連線中的客戶端。
- **FR-2.3 狀態計算**：前端接收到進度後，透過 `(自身號碼 - 目前叫號 = 前方人數)` 自行呈現等待進度。

### 3.3 放行與業務處理 (Dequeue & Process)
- **FR-3.1 動態放行配置**：管理員可針對各活動設定「放行模式（手動 MANUAL / 自動 AUTOMATIC）」，以及「單次放行最大數量 (Batch Size)」，靈活應對不同業務場景。
- **FR-3.2 批次放行**：
  - **自動模式**：背景 Worker 根據剩餘容量，定時從 Redis `ZSET` 取出特定數量的使用者名單，並更新已放行最大號碼。
  - **手動模式**：管理員可透過儀表板點擊手動放行按鈕，精準控制單次放行的人數。
- **FR-3.3 授權事件發布**：系統將放行名單封裝為授權事件送入 Kafka。
- **FR-3.4 業務處理與通知**：核心微服務消費 Kafka 事件進行處理，完成後透過 SSE 通知前端跳轉至結帳或劃位區。

### 3.4 監控與儀表板 (Monitoring & Dashboard)
- **FR-4.1 容量水位監控**：管理員可即時監控下游系統的「最大總容量 (Max Capacity)」與「目前剩餘空位 (Available Capacity)」。
- **FR-4.2 排隊進度監控**：即時掌握當前「已發放的最大號碼」、「目前處理到的號碼」以及「正在排隊中的總人數」。

---

## 4. 非功能性需求 (Non-Functional Requirements)

### 4.1 效能與高併發 (Performance)
- **降級 ACID 保證**：排隊取號階段捨棄 RDBMS 的 ACID 特性，避免 Row-Level Lock 競爭與 Connection Pool 耗盡，將狀態維護降級至記憶體級別（Redis），實現次毫秒級的回應速度。
- **響應式推播**：SSE 推播層必須使用 Spring WebFlux，以非阻塞 I/O 支撐數萬至數十萬級別別的併發長連線。

### 4.2 可用性與容錯機制 (Availability & Reliability)
- **連線與狀態解耦**：使用者的排隊狀態綁定於 Redis，不因網路斷線遺失。斷線重連時只需提供 Token 即可恢復狀態。
- **心跳與過期淘汰 (TTL)**：排隊 Token 需設定 TTL（如 30 秒）。前端需定時發送 Ping 請求續命，超時未回應視為離線，自動清除殭屍佔位。
- **重連退避策略**：前端實作指數退避（Exponential Backoff）與隨機延遲（Jitter）重連機制，防止斷線瞬間伺服器遭遇雪崩式連線。

### 4.3 核心資料強一致性 (Data Consistency)
- **防超賣機制**：實際業務處理（如扣庫存、劃位）時，必須依賴 RDBMS 的樂觀鎖（Optimistic Locking）作為最終防線。
- **保證遞送 (At-Least-Once)**：使用 **Outbox Pattern**，在同一個 Local Transaction 中將業務資料與事件寫入 RDBMS，並透過 CDC 或排程非同步推入 Kafka，徹底解決雙寫不一致的問題。

---

## 5. 架構決策與邊界界定 (Architecture Decisions)

為達到最佳效能，本系統在架構邊界上做出了嚴格的切割：

### 5.1 排隊邊界層 (Queueing Layer)
- **決策**：保持極簡，**不引入 CQRS 與 Event Sourcing (ES)**。
- **理由**：排隊資料屬於「過渡性資料 (Ephemeral Data)」，生命週期極短。紀錄每一次的排隊推移事件會癱瘓系統且毫無稽核價值。因此單純依靠 Redis 的 `ZSET` 與 `INCR` 即可滿足需求。

### 5.2 核心領域層 (Core Domain Layer)
- **決策**：**預設採用 CQRS + Outbox Pattern**。
- **理由**：進入核心業務（如訂單、劃位）後，讀寫比例懸殊且規則複雜。透過 CQRS 分離讀取（Redis/ES）與寫入（RDBMS）模型。考量到完全導入 Event Sourcing 開發心智負擔過大且存在最終一致性挑戰，本系統採取「CQRS 搭配 Outbox Pattern」，以較低的成本獲得事件可靠遞送與微服務解耦的核心優勢。
