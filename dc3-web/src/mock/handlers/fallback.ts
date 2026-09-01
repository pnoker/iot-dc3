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

import {ok, okArray, okPage, responseOf} from '../response';
import type {Handler} from '../types';

/** Endpoints that return a flat array (not a PageResult) on success. */
const ARRAY_SUFFIXES = /(_by_ids|_list_by_|list_tree)$/;

const CREATED = '2026-07-15T09:30:00';
const UPDATED = '2026-08-01T14:20:00';

/**
 * Generic 3-row stub for unregistered auth/manager list endpoints (most of the
 * settings family) so those tables aren't blank. Field names stay broad
 * (id/name/enableFlag/createTime/operateTime/remark) to satisfy common columns;
 * entity-specific columns may render empty, which is fine for a demo.
 */
const stubPage = (url: string) => {
  const entity = url.split('/').slice(-2, -1)[0] || 'entity';
  const items = Array.from({length: 3}, (_, i) => ({
    id: `mock-${entity}-${i + 1}`,
    name: `Mock ${entity} ${i + 1}`,
    enableFlag: 'ENABLE',
    createTime: CREATED,
    operateTime: UPDATED,
    remark: 'Demo data',
  }));
  return okPage(items);
};

/**
 * Last-resort handler: shape the response by URL suffix so unregistered
 * endpoints (most of the settings family, dashboard insight cards) degrade
 * gracefully instead of surfacing server errors. Every business page wraps its
 * calls in try/catch, so empty payloads keep the UI intact.
 */
export const fallbackHandler: Handler = (ctx) => {
  const {url, params, body} = ctx;

  if (url.endsWith('/list')) {
    // Settings/manager tables get stubbed rows; data-domain lists (point_value,
    // command_history, …) stay empty since their consumers expect specific shapes.
    const isConfigDomain = url.startsWith('api/v3/auth/') || url.startsWith('api/v3/manager/');
    return responseOf(ctx.config, isConfigDomain ? stubPage(url) : okPage([] as Record<string, unknown>[], 0));
  }
  if (ARRAY_SUFFIXES.test(url)) {
    return responseOf(ctx.config, okArray([]));
  }
  if (url.endsWith('/get_by_id')) {
    return responseOf(
      ctx.config,
      ok({id: String(params.id ?? '0'), name: 'Mock Entity', enableFlag: 'ENABLE'}),
    );
  }
  if (/(\/(add|update|delete))$/.test(url)) {
    const id = (body && (body.id ?? body.name)) || 'mock-id';
    return responseOf(ctx.config, ok(String(id)));
  }
  if (url.endsWith('/count') || /get_count_by_/.test(url)) {
    return responseOf(ctx.config, ok(0));
  }
  return responseOf(ctx.config, ok({}));
};
