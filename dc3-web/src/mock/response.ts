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

/**
 * Build a minimal AxiosResponse, mirroring the adapter contract verified in
 * tests/unit/axios.test.ts. The response interceptor returns response.data
 * verbatim when ok===true, so `data` here MUST be the full R envelope — never
 * the inner payload.
 */
export const responseOf = (
  config: InternalAxiosRequestConfig,
  data: unknown,
  status = 200,
): AxiosResponse => ({
  data,
  status,
  statusText: String(status),
  headers: {},
  config,
  request: {},
});

/**
 * R envelope constructors. The interceptor gates success on the `ok` boolean
 * alone; code is always '0' and must never be 'R4031'/'R4032' (those trigger
 * the change-password pass-through in the login flow).
 */
export const ok = <T = unknown>(data: T): R<T> => ({ok: true, code: '0', message: 'success', data});

export const okPage = <T>(records: T[], total = records.length): R<{ total: number; records: T[] }> =>
  ok({total, records});

export const okArray = <T>(data: T[]): R<T[]> => ok(data);
