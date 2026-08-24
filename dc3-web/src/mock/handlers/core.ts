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

import {on} from '../dispatch';
import {localizeEntities, localizeEntity} from '../locale';
import {matches, paginate} from '../query';
import {ok, responseOf} from '../response';
import {newId, stamp} from '../crud';

const enableFilter = (row: Record<string, unknown>, body: any): boolean => {
  const flag = body?.enableFlag;
  if (!flag || flag === 'ALL') return true;
  return row.enableFlag === flag;
};

const findById = (rows: Record<string, unknown>[], id: unknown) =>
  rows.find((r) => String(r.id) === String(id));

const byIdsMap = (rows: Record<string, unknown>[], ids: unknown[]): Record<string, unknown> => {
  const map: Record<string, unknown> = {};
  for (const id of ids) {
    // Demo fallback: unknown ids resolve to the first row so charts stay
    // populated instead of silently dropping a lookup. Acceptable for mock data.
    map[String(id)] = findById(rows, id) ?? rows[0];
  }
  return map;
};

/** Build an id→status map shaped like the real status/list response. */
const statusMap = (rows: Record<string, unknown>[]): Record<string, string> =>
  Object.fromEntries(
    rows.map((r, i) => [String(r.id), i % 5 === 0 ? 'OFFLINE' : i % 11 === 0 ? 'FAULT' : 'ONLINE']),
  );

export function registerCoreHandlers(): void {
  // ── driver ──
  on('post', 'api/v3/manager/driver/list', (ctx) => {
    const rows = localizeEntities('driver', ctx.db.drivers);
    const filter = (d: Record<string, unknown>) =>
      matches(d.driverName, ctx.body.driverName) &&
      matches(d.serviceName, ctx.body.serviceName) &&
      matches(d.serviceHost, ctx.body.serviceHost) &&
      enableFilter(d, ctx.body);
    return responseOf(ctx.config, ok(paginate(rows, ctx.body, filter)));
  });
  on('get', 'api/v3/manager/driver/get_by_id', (ctx) =>
    responseOf(
      ctx.config,
      ok(localizeEntity('driver', findById(ctx.db.drivers, ctx.params.id) ?? ctx.db.drivers[0])),
    ),
  );
  on('post', 'api/v3/manager/driver/list_by_ids', (ctx) =>
    responseOf(
      ctx.config,
      ok(byIdsMap(localizeEntities('driver', ctx.db.drivers), Array.isArray(ctx.body) ? ctx.body : [])),
    ),
  );

  // ── device ──
  on('post', 'api/v3/manager/device/list', (ctx) => {
    const rows = localizeEntities('device', ctx.db.devices);
    const filter = (d: Record<string, unknown>) =>
      matches(d.deviceName, ctx.body.deviceName) &&
      (!ctx.body.driverId || String(d.driverId) === String(ctx.body.driverId)) &&
      enableFilter(d, ctx.body);
    return responseOf(ctx.config, ok(paginate(rows, ctx.body, filter)));
  });
  on('get', 'api/v3/manager/device/get_by_id', (ctx) =>
    responseOf(
      ctx.config,
      ok(localizeEntity('device', findById(ctx.db.devices, ctx.params.id) ?? ctx.db.devices[0])),
    ),
  );
  on('post', 'api/v3/manager/device/list_by_ids', (ctx) =>
    responseOf(
      ctx.config,
      ok(byIdsMap(localizeEntities('device', ctx.db.devices), Array.isArray(ctx.body) ? ctx.body : [])),
    ),
  );

  // ── profile ──
  on('post', 'api/v3/manager/profile/list', (ctx) => {
    const rows = localizeEntities('profile', ctx.db.profiles);
    const filter = (p: Record<string, unknown>) =>
      matches(p.profileName, ctx.body.profileName) && enableFilter(p, ctx.body);
    return responseOf(ctx.config, ok(paginate(rows, ctx.body, filter)));
  });
  on('get', 'api/v3/manager/profile/get_by_id', (ctx) =>
    responseOf(
      ctx.config,
      ok(localizeEntity('profile', findById(ctx.db.profiles, ctx.params.id) ?? ctx.db.profiles[0])),
    ),
  );
  on('post', 'api/v3/manager/profile/list_by_ids', (ctx) =>
    responseOf(
      ctx.config,
      ok(byIdsMap(localizeEntities('profile', ctx.db.profiles), Array.isArray(ctx.body) ? ctx.body : [])),
    ),
  );

  // ── point ──
  on('post', 'api/v3/manager/point/list', (ctx) => {
    const rows = localizeEntities('point', ctx.db.points);
    const filter = (p: Record<string, unknown>) =>
      matches(p.pointName, ctx.body.pointName) &&
      (!ctx.body.profileId || String(p.profileId) === String(ctx.body.profileId)) &&
      enableFilter(p, ctx.body);
    return responseOf(ctx.config, ok(paginate(rows, ctx.body, filter)));
  });
  on('get', 'api/v3/manager/point/get_by_id', (ctx) =>
    responseOf(
      ctx.config,
      ok(localizeEntity('point', findById(ctx.db.points, ctx.params.id) ?? ctx.db.points[0])),
    ),
  );
  on('post', 'api/v3/manager/point/list_by_ids', (ctx) =>
    responseOf(
      ctx.config,
      ok(byIdsMap(localizeEntities('point', ctx.db.points), Array.isArray(ctx.body) ? ctx.body : [])),
    ),
  );

  // ── runtime status (consumed as Record<id, ONLINE/OFFLINE/FAULT>) ──
  on('post', 'api/v3/data/driver/status/list', (ctx) =>
    responseOf(ctx.config, ok(statusMap(ctx.db.drivers))),
  );
  on('post', 'api/v3/data/device/status/list', (ctx) =>
    responseOf(ctx.config, ok(statusMap(ctx.db.devices))),
  );

  // ── CUD for mutable entities (driver stays read-only — backend-registered) ──
  const cud = (url: string, key: 'devices' | 'profiles' | 'points') => {
    on('post', `${url}/add`, (ctx) => {
      const row: Record<string, unknown> = {...ctx.body, id: newId(), createTime: stamp(), operateTime: stamp()};
      ctx.db[key].push(row);
      return responseOf(ctx.config, ok(String(row.id)));
    });
    on('post', `${url}/update`, (ctx) => {
      const coll = ctx.db[key];
      const i = coll.findIndex((r) => String(r.id) === String(ctx.body?.id));
      if (i >= 0) coll[i] = {...coll[i], ...ctx.body, operateTime: stamp()};
      return responseOf(ctx.config, ok(String(ctx.body?.id ?? '')));
    });
    on('post', `${url}/delete`, (ctx) => {
      const coll = ctx.db[key];
      const i = coll.findIndex((r) => String(r.id) === String(ctx.params.id));
      if (i >= 0) coll.splice(i, 1);
      return responseOf(ctx.config, ok(String(ctx.params.id)));
    });
  };
  cud('api/v3/manager/device', 'devices');
  cud('api/v3/manager/profile', 'profiles');
  cud('api/v3/manager/point', 'points');
}
