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

import type {AxiosAdapter, InternalAxiosRequestConfig} from 'axios';

import {db} from './db';
import {resolve} from './dispatch';
import type {MockCtx} from './types';

const safeParse = (raw: unknown): any => {
  if (typeof raw !== 'string') return raw ?? {};
  try {
    return JSON.parse(raw);
  } catch {
    return {};
  }
};

/**
 * Axios adapter that short-circuits every request to the mock dispatch table.
 * Runs after the request interceptor (which injects auth headers) and feeds
 * its response back through the response interceptor, so the rest of the app
 * is unaware anything is mocked.
 */
export const createMockAdapter = (): AxiosAdapter => async (config: InternalAxiosRequestConfig) => {
  const ctx: MockCtx = {
    config,
    url: (config.url || '').replace(/^\/+/, ''),
    method: (config.method || 'get').toLowerCase(),
    params: (config.params as Record<string, unknown>) || {},
    body: safeParse(config.data),
    db,
  };
  const handler = resolve(ctx);
  return handler(ctx);
};
