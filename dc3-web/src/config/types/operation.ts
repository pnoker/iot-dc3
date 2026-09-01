export type OperationStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | 'EXPIRED';

export interface OperationAccepted {
  operationId: string;
  statusUri: string;
}

export interface OperationView {
  operationId: string;
  status: OperationStatus;
  progress: number;
  result: unknown;
  error: unknown;
  createdAt: string;
  updatedAt: string;
  expiresAt: string | null;
}

export type OperationUiStatus = OperationStatus | 'REQUEST_ERROR';
