package com.example.qms.application.port.in;

import com.example.qms.application.command.SetCapacityCommand;

import java.util.concurrent.CompletableFuture;

/**
 * 【Inbound Port】設定系統總容量埠 (Use Case)
 * 
 * 負責處理管理員動態修改系統最大總容量 (Max Capacity) 的業務邏輯。
 * 設定時會自動重算剩餘容量，以確保系統水位正確。
 */
public interface SetCapacityUseCase {

    /**
     * 重新設定指定租戶與活動的下游系統最大總容量。
     *
     * @param command 包含租戶、活動識別碼與新容量值的命令物件
     * @return 處理完畢的 CompletableFuture
     */
    CompletableFuture<Void> setCapacity(SetCapacityCommand command);
}
