# QMS SaaS 商業化 Backlog

本文件記錄了將 QMS (Queueing Management System) 從目前的「技術 MVP」演進為「完整商業化 SaaS 產品」所需的關鍵待辦事項 (Backlog)。
目前的架構已經完成了最困難的高併發排隊與原子性放行引擎，未來的開發重點將聚焦於租戶隔離、活動管理、安全性與動態調度。

## 📌 Epics & Features

### 1. 租戶管理模組 (Tenant Service)
作為 SaaS 平台，必須具備多租戶 (Multi-tenancy) 的帳號與權限隔離機制。
- [ ] **租戶註冊與生命週期管理**：實作租戶的註冊、登入、登出，並核發對應的 `tenantId`。
- [ ] **訂閱方案與計費機制**：支援不同的訂閱等級 (如 Free, Pro, Enterprise)，限制可開啟的活動數量或最大流量。
- [ ] **API Key / Client Secret 發放**：為每個租戶核發專屬的金鑰，用於驗證伺服器對伺服器 (S2S) 的 API 呼叫（如釋放容量 `Release Capacity`、驗證 `Queue Token`）。
- [ ] **安全與防護設定**：允許租戶設定 CORS 白名單網域，防止未授權網站嵌入或盜用排隊系統。

### 2. 活動管理模組 (Activity Management Service)
允許租戶自助建立與管理其專屬的搶購活動。
- [ ] **活動 CRUD 管理**：實作建立、編輯、暫停、結束活動的功能，並核發 `activityId`。
- [ ] **排程與自動化控制**：支援設定活動的「開始排隊時間」與「正式搶購時間」，時間到達前禁止取號或放行。
- [ ] **客製化 UI (White-labeling)**：提供介面讓租戶上傳專屬 Logo、設定排隊頁面的主視覺配色與背景，提升消費者的品牌一致性體驗。

### 3. 動態排程與調度機制 (Dynamic Scheduling)
將目前寫死 (`demo-tenant`) 的背景 Worker 改造成支援百萬級多租戶的分散式調度架構。
- [ ] **動態活動輪詢 (Active Activity Polling)**：`DequeueTicketWorker` 與 `AbandonedSessionReclaimerWorker` 需動態從資料庫讀取「進行中 (Active)」的活動清單，並依序或並行處理。
- [ ] **分散式任務排程 (Distributed Task Scheduling)**：引入如 Quartz、Redisson 分散式鎖或 Spring Cloud Task 等機制，當租戶達到千百個時，讓多台 Worker 節點能分片 (Sharding) 處理不同租戶的放行任務，避免單點效能瓶頸。

### 4. API 閘道器與安全性 (API Gateway & Security)
保護 SaaS 平台免受惡意攻擊，並實施租戶級別的流量管控。
- [ ] **導入 API Gateway**：如 Spring Cloud Gateway，作為所有進出流量的統一入口。
- [ ] **身分驗證攔截**：在 Gateway 層統一處理 JWT 驗證與 API Key 檢查，降低微服務內部的實作複雜度。
- [ ] **流量限速 (Rate Limiting)**：依據租戶的訂閱等級，在 Gateway 層實施 API 呼叫頻率限制，防禦 DDoS 攻擊並確保平台整體可用性。
