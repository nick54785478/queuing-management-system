import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../services/admin.service';

/**
 * 管理員儀表板元件 (Admin Dashboard)
 * 提供給系統管理員，用來動態設定排隊系統的核心運作模式與放行參數。
 */
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  /** 每次放行的號碼數量 (預設為 10) */
  dequeueBatchSize: number = 10;
  /** 放行模式，分為 AUTOMATIC (全自動) 或 MANUAL (手動) */
  mode: string = 'AUTOMATIC';
  /** 是否啟用每日自動歸零，讓系統在過午夜後自動從 1 號開始發放 */
  resetDaily: boolean = true;
  /** 控制儲存按鈕的 loading 狀態 */
  isSaving: boolean = false;
  /** 控制儲存成功訊息的顯示 */
  saveSuccess: boolean = false;

  /** 當前可用容量 (剩餘空位) */
  currentCapacity: number = 0;
  /** 當前設定的系統最大總容量 */
  maxCapacity: number = 0;
  /** 欲重置/注入的目標最大容量 */
  targetCapacity: number = 100;
  /** 控制容量重置按鈕的 loading 狀態 */
  isResetting: boolean = false;
  /** 控制容量重置成功訊息的顯示 */
  resetSuccess: boolean = false;

  private pollingInterval: any;

  constructor(private adminService: AdminService) {}

  ngOnInit() {
    this.loadConfig();
    this.loadCapacity();
    
    // 每 2 秒自動刷新一次容量狀態
    this.pollingInterval = setInterval(() => {
      this.loadCapacity();
    }, 2000);
  }

  ngOnDestroy() {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
    }
  }

  /**
   * 初始化時向後端拉取最新的系統容量
   */
  loadCapacity() {
    this.adminService.getCapacity().subscribe({
      next: (res) => {
        this.currentCapacity = res.remainingCapacity;
        this.maxCapacity = res.maxCapacity;
        // 如果 targetCapacity 還沒有被使用者改過，就用後端傳來的值
        if (this.targetCapacity === 100 && this.maxCapacity !== 0) {
          this.targetCapacity = this.maxCapacity;
        }
      },
      error: (err) => console.error('Failed to load capacity', err)
    });
  }

  /**
   * 初始化時向後端拉取最新的系統設定
   */
  loadConfig() {
    this.adminService.getConfig().subscribe({
      next: (config) => {
        this.dequeueBatchSize = config.dequeueBatchSize;
        this.mode = config.mode || 'AUTOMATIC';
        this.resetDaily = config.resetDaily;
      },
      error: (err) => console.error('Failed to load config', err)
    });
  }

  /**
   * 將畫面上調整好的新設定儲存回後端
   */
  saveConfig() {
    this.isSaving = true;
    this.saveSuccess = false;
    this.adminService.updateConfig({ 
      dequeueBatchSize: this.dequeueBatchSize,
      mode: this.mode,
      resetDaily: this.resetDaily
    }).subscribe({
      next: (res) => {
        // 更新成功後，以回傳的最新資料為準
        this.dequeueBatchSize = res.dequeueBatchSize;
        this.mode = res.mode;
        this.resetDaily = res.resetDaily;
        this.isSaving = false;
        this.saveSuccess = true;
        // 成功訊息顯示 3 秒後自動隱藏
        setTimeout(() => this.saveSuccess = false, 3000);
      },
      error: (err) => {
        console.error('Failed to save config', err);
        this.isSaving = false;
      }
    });
  }

  /**
   * 重置或覆蓋當前的最大總容量
   */
  resetCapacity() {
    this.isResetting = true;
    this.resetSuccess = false;
    this.adminService.resetCapacity(this.targetCapacity).subscribe({
      next: () => {
        this.isResetting = false;
        this.resetSuccess = true;
        // 重新載入以獲取正確的剩餘容量
        this.loadCapacity();
        // 成功訊息顯示 3 秒後自動隱藏
        setTimeout(() => this.resetSuccess = false, 3000);
      },
      error: (err) => {
        console.error('Failed to update max capacity', err);
        this.isResetting = false;
      }
    });
  }
}
