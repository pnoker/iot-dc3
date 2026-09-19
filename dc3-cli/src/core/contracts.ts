/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
/**
 * Sort field and direction applied to list queries.
 */
export interface SortSpec {
  field: string;
  direction: 'ASC' | 'DESC';
}

/**
 * Offset-based paging request shared by list endpoints.
 */
export interface PageRequest {
  offset: number;
  limit: number;
  sort?: SortSpec[];
}

/**
 * Offset-paged response wrapper over records.
 */
export interface OffsetPage<T> {
  items: T[];
  offset: number;
  limit: number;
  total: number;
  hasNext: boolean;
}

/**
 * Cursor-paged response wrapper for large history scans.
 */
export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
  hasNext: boolean;
}

/**
 * RFC 7807 problem details returned on gateway errors.
 */
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

/**
 * HTTP 202 Accepted envelope for asynchronous operations.
 */
export interface OperationAccepted {
  operationId: string;
  statusUri: string;
}

/**
 * Lifecycle status of an asynchronous operation.
 */
export type OperationStatus =
  'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | 'EXPIRED';

/**
 * Operation progress view polled from the status endpoint.
 */
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
