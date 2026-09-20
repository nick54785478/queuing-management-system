# QMS 本地開發基礎設施 (Docker Compose)

本目錄包含 QMS (Queuing Management System) 在本地開發所需的依賴中間件。
只需要執行以下指令，即可一鍵啟動所有所需的資料庫與中介軟體：

```bash
docker-compose up -d
```

## 包含的服務

本配置會自動啟動以下服務：
1. **PostgreSQL** (`qms-postgres:5432`): 關聯式資料庫。
2. **Kafka (KRaft 模式)** (`qms-kafka:9092`): 訊息佇列，負責處理非同步事件。
3. **Kafka-UI** (`qms-kafka-ui:8090`): 視覺化的 Kafka 管理介面。
4. **Redis Cluster** (`qms-redis-node-0`, `1`, `2`): 3 節點的微型 Redis 叢集，負責網關限流與分散式快取。

---

## 關於 `qms-redis-cluster-init` 容器 (重要說明)

在執行 `docker-compose up -d` 後，您會在 Docker Desktop 列表中看到一個名為 `qms-redis-cluster-init` 的容器處於**已停止 (Exited)** 狀態，這是**正常現象，請勿擔心，也建議不要刪除**。

**它的用途是什麼？**
因為 Bitnami Redis Cluster 映像檔在使用固定 IP 配置時，無法自動正確初始化叢集，這個 `init` 容器扮演了自動化修復的腳色。
它是一個**一次性任務 (One-shot task)**：
1. 在啟動時會等待 10 秒鐘（等待另外 3 個 Redis 主節點完全啟動）。
2. 自動執行 `redis-cli --cluster create` 指令，將 3 個節點綁定為正確的叢集，並分配所有 16384 個 Slot。
3. 任務完成後，**它會自動退出並釋放所有資源 (0 記憶體/CPU 消耗)**。

保留它，可以確保未來任何新成員接手專案，或是當您清除了本地的 Docker Volumes 時，都能夠達到「一鍵全自動啟動」，而不需要再手動介入修復叢集狀態。

## 資料持久化 (Volumes)

本配置全面採用 Docker 的 **Named Volumes (命名資料卷)** 來確保您的資料持久化，並具備 100% 的跨平台可攜性。
所有資料庫的檔案皆由 Docker 底層安全管理，您無須擔心路徑綁定錯誤的問題。
