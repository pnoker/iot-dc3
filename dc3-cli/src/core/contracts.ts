export interface SortSpec {
  field: string;
  direction: 'ASC' | 'DESC';
}

export interface PageRequest {
  offset: number;
  limit: number;
  sort?: SortSpec[];
}

export interface OffsetPage<T> {
  items: T[];
  offset: number;
  limit: number;
  total: number;
  hasNext: boolean;
}

export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
  hasNext: boolean;
}

export interface ProblemDetails {
  type: string;
  title: string;
  status: number;
  code: string;
  detail?: string;
  instance?: string;
  traceId?: string;
  errors?: Record<string, string[]>;
}

export interface OperationAccepted {
  operationId: string;
  statusUri: string;
}

export type OperationStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED'
  | 'EXPIRED';

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
