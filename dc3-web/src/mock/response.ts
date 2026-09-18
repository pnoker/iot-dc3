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

export interface ProblemDetails {
  type: string;
  title: string;
  status: number;
  code: string;
  detail: string;
}

export const ok = <T = unknown>(data: T): T => data;

export const fail = (code: string, message: string, status = 400): ProblemDetails => ({
  type: 'about:blank',
  title: message,
  status,
  code,
  detail: message,
});

export const okPage = <T>(items: T[], total = items.length, offset = 0, limit = items.length || 1): PageResult<T> => ({
  items,
  offset,
  limit,
  total,
  hasNext: offset + items.length < total,
});

export const okArray = <T>(data: T[]): T[] => data;
