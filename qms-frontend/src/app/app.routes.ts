import { Routes } from '@angular/router';
import { TicketBoardComponent } from './features/ticket/components/ticket-board/ticket-board.component';
import { AdminDashboardComponent } from './features/admin/components/admin-dashboard/admin-dashboard.component';
import { OperatorDashboardComponent } from './features/operator/components/operator-dashboard/operator-dashboard.component';

export const routes: Routes = [
  { path: '', component: TicketBoardComponent },
  { path: 'admin', component: AdminDashboardComponent },
  { path: 'operator', component: OperatorDashboardComponent },
  { path: '**', redirectTo: '' }
];
