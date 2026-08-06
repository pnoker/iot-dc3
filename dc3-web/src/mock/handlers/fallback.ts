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
const ARRAY_SUFFIXES = /(_by_ids|_list_by_|list_tree|\/list)$/;

/**
 * Last-resort handler: shape the response by URL suffix so unregistered
 * endpoints (most of the settings family, dashboard insight cards) degrade
 * gracefully instead of surfacing server errors. Every business page wraps its
 * calls in try/catch, so empty payloads keep the UI intact.
 */
export const fallbackHandler: Handler = (ctx) => {
  const {url, params, body} = ctx;

  if (url.endsWith('/list')) {
    return responseOf(ctx.config, okPage([] as Record<string, unknown>[], 0));
  }
  if (ARRAY_SUFFIXES.test(url)) {
    return responseOf(ctx.config, okArray([]));
  }
  if (url.endsWith('/get_by_id')) {
    return responseOf(
      ctx.config,
      ok({id: String(params.id ?? '0'), name: 'Mock Entity', enableFlag: 'ENABLED'}),
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
