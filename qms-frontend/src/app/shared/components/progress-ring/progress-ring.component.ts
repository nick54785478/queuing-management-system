import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-progress-ring',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './progress-ring.component.html',
  styleUrl: './progress-ring.component.css'
})
export class ProgressRingComponent {
  @Input() totalPeopleAhead: number = 0;
  @Input() currentServingNumber: number = 0;
  @Input() myTicketNumber: number = 0;

  get progressPercentage(): number {
    if (this.myTicketNumber === 0 || this.currentServingNumber === 0) return 0;
    // Simple logic: if myTicketNumber is 100, and current is 50, I have 50 ahead.
    // Assuming start was 1 for percentage calculation (or just an arbitrary UI fill)
    const total = this.myTicketNumber;
    const progress = Math.min((this.currentServingNumber / total) * 100, 100);
    return Math.max(progress, 0);
  }

  get strokeDashoffset(): number {
    const circumference = 2 * Math.PI * 45; // radius 45
    return circumference - (this.progressPercentage / 100) * circumference;
  }
}
