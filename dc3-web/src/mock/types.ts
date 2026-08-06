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

import type {MockDb} from './db';

export interface MockCtx {
  /** Original axios config — needed to build the AxiosResponse. */
  config: InternalAxiosRequestConfig;
  /** Normalized URL with no leading slash (e.g. `api/v3/manager/device/list`). */
  url: string;
  /** Lowercased HTTP method. */
  method: string;
  /** Query params (GET) or empty object. */
  params: Record<string, unknown>;
  /** Parsed request body (POST) or empty object. */
  body: any;
  /** Mutable in-memory store shared across handlers. */
  db: MockDb;
}

export type Handler = (ctx: MockCtx) => AxiosResponse | Promise<AxiosResponse>;

export interface RouteRule {
  method?: string;
  pattern: RegExp;
  handler: Handler;
}
