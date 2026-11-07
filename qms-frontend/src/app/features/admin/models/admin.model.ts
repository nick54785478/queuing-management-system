export interface UpdateQueueConfigResource {
  dequeueBatchSize: number;
  mode: string;
  resetDaily: boolean;
}

export interface QueueConfigRetrievedResource {
  dequeueBatchSize: number;
  mode: string;
  resetDaily: boolean;
}