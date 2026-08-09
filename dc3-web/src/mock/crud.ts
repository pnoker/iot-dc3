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

import {on} from './dispatch';
import {matches, paginate} from './query';
import {ok, responseOf} from './response';
import type {MockDb} from './db';
import {db} from './db';
import type {MockCtx} from './types';

/** Format the current time as `YYYY-MM-DD HH:mm:ss` so demo rows read naturally. */
export const stamp = (): string => {
  const d = new Date();
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
};

/** Monotonic id for rows created at runtime — starts above the seed id space. */
let counter = 90001;
export const newId = (): string => String(counter++);

export interface CrudSpec {
  /** Base URL without leading slash, e.g. `api/v3/auth/role`. */
  baseUrl: string;
  /** Mutable collection in `db` backing this entity. */
  collection: keyof MockDb;
  /** Body fields matched as case-insensitive substrings in `/list`. */
  search?: string[];
  /** Body fields matched by equality in `/list` (defaults to `['enableFlag']`). */
  exact?: string[];
  /** Register `/enable` and `/disable` toggles (principal/serviceAccount). */
  enable?: boolean;
}

const compileFilter =
  (search: string[], exact: string[]) =>
    (row: Record<string, unknown>, body: any): boolean =>
      search.every((f) => matches(row[f], body?.[f])) &&
      exact.every((f) => {
        const v = body?.[f];
        return v === undefined || v === null || v === '' || v === 'ALL' || String(row[f]) === String(v);
      });

/**
 * Register the standard CRUD endpoints for an entity against the mutable in-memory
 * `db`. add/update/delete/enable/disable mutate the collection so a demo session
 * reflects user actions; `/list` re-reads the (now mutated) collection each call.
 *
 * Registered: POST /list, GET /get_by_id, POST /add, POST /update, POST /delete,
 * (optional) POST /enable, POST /disable.
 *
 * Out of scope (entity-specific shapes): /list_by_ids, /list_tree, join queries,
 * history — register those separately next to this call.
 */
export function registerCrud(spec: CrudSpec): void {
  const {baseUrl, collection, search = [], exact = ['enableFlag'], enable = false} = spec;
  const rows = (): Record<string, unknown>[] => db[collection] as Record<string, unknown>[];
  const filter = compileFilter(search, exact);
  const findById = (id: unknown) => rows().find((r) => String(r.id) === String(id));

  on('post', `${baseUrl}/list`, (ctx) => responseOf(ctx.config, ok(paginate(rows(), ctx.body, filter))));
  on('get', `${baseUrl}/get_by_id`, (ctx) =>
    responseOf(ctx.config, ok(findById(ctx.params.id) ?? rows()[0] ?? {})),
  );
  on('post', `${baseUrl}/add`, (ctx) => {
    const row: Record<string, unknown> = {...ctx.body, id: newId(), createTime: stamp(), operateTime: stamp()};
    rows().push(row);
    return responseOf(ctx.config, ok(String(row.id)));
  });
  on('post', `${baseUrl}/update`, (ctx) => {
    const i = rows().findIndex((r) => String(r.id) === String(ctx.body?.id));
    if (i >= 0) rows()[i] = {...rows()[i], ...ctx.body, operateTime: stamp()};
    return responseOf(ctx.config, ok(String(ctx.body?.id ?? '')));
  });
  on('post', `${baseUrl}/delete`, (ctx) => {
    const id = ctx.params.id;
    const i = rows().findIndex((r) => String(r.id) === String(id));
    if (i >= 0) rows().splice(i, 1);
    return responseOf(ctx.config, ok(String(id)));
  });
  if (enable) {
    const toggle = (flag: string) => (ctx: MockCtx) => {
      const row = findById(ctx.params.id);
      if (row) row.enableFlag = flag;
      return responseOf(ctx.config, ok(String(ctx.params.id)));
    };
    on('post', `${baseUrl}/enable`, toggle('ENABLE'));
    on('post', `${baseUrl}/disable`, toggle('DISABLE'));
  }
}
