export interface TicketEnqueuedResource {
  ticketId: string;
  ticketNumber: number;
  queueToken: string;
  status: string;
}

export interface TicketProgressRetrievedResource {
  currentServingNumber: number;
}
