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

import type {AxiosResponse, InternalAxiosRequestConfig} from 'axios';
import type {PageResult} from '@/config/types';

/**
 * Build a mock response envelope with the given status.
 * @param config - configuration object
 * @param data - payload to send or render
 * @param status - status entries
 * @returns the mock envelope for the given status
 */
export const responseOf = (
  config: InternalAxiosRequestConfig,
  data: unknown,
  status = 200,
): AxiosResponse => ({
  data,
  status,
  statusText: String(status),
  headers: {'content-type': status >= 400 ? 'application/problem+json' : 'application/json'},
  config,
  request: {},
});

/**
 * ProblemDetails data contract.
 */
export interface ProblemDetails {
  type: string;
  title: string;
  status: number;
  code: string;
  detail: string;
}

/**
 * Build a mock success envelope.
 * @param data - payload to send or render
 * @returns a mock success envelope wrapping the payload
 */
export const ok = <T = unknown>(data: T): T => data;

/**
 * Build a mock failure envelope.
 * @param code - business status code
 * @param message - message text
 * @param status - status entries
 * @returns a mock failure envelope with the message
 */
export const fail = (code: string, message: string, status = 400): ProblemDetails => ({
  type: 'about:blank',
  title: message,
  status,
  code,
  detail: message,
});

/**
 * Build a mock success envelope for one page of rows.
 * @param items - items to process
 * @param total - total row count
 * @param offset - page offset
 * @param limit - maximum number of entries to return or generate
 * @returns a mock success envelope with one page of rows
 */
export const okPage = <T>(items: T[], total = items.length, offset = 0, limit = items.length || 1): PageResult<T> => ({
  items,
  offset,
  limit,
  total,
  hasNext: offset + items.length < total,
});

/**
 * Build a mock success envelope wrapping an array.
 * @param data - payload to send or render
 * @returns a mock success envelope wrapping the array
 */
export const okArray = <T>(data: T[]): T[] => data;
