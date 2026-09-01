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
import {paginate} from '../query';
import {fail, ok, responseOf} from '../response';
import {db} from '../db';

const dec = (p: Record<string, unknown>): number => Number(p.valueDecimal ?? 2);

/** Deterministic pseudo-random in [0,1) from a string seed — stable across reloads. */
const hash = (s: string | number): number => {
  let h = 2166136261;
  for (const c of String(s)) h = (h ^ c.charCodeAt(0)) * 16777619;
  return (h >>> 0) / 4294967296;
};

/** Pick a base/amplitude for a realistic waveform from the point's unit/name. */
const range = (point: Record<string, unknown>): { base: number; amp: number } => {
  const u = String(point.unit ?? '');
  const n = String(point.pointName ?? '');
  if (u.includes('℃') || u.includes('°C') || n.includes('温')) return {base: 25, amp: 5};
  if (u.includes('%RH') || n.includes('湿')) return {base: 55, amp: 10};
  if (u.includes('kW')) return {base: 2.2, amp: 0.5};
  if (u.includes('V') || n.includes('电压')) return {base: 220, amp: 6};
  if (u.includes('A') || n.includes('电流')) return {base: 10, amp: 2};
  if (u.includes('%') || n.includes('率')) return {base: 60, amp: 18};
  if (u.includes('m³')) return {base: 120, amp: 30};
  return {base: 50, amp: 12};
};

const valueAt = (point: Record<string, unknown>, hoursAgo: number): number => {
  const {base, amp} = range(point);
  const id = String(point.id);
  const wave = Math.sin(hoursAgo / 3.5 + hash(id) * 6.283) * amp;
  const noise = (hash(`${id}-${hoursAgo}`) - 0.5) * amp * 0.2;
  return Number((base + wave + noise).toFixed(dec(point)));
};

const stamp = (ms: number): string => {
  const t = new Date(ms);
  const p = (n: number) => String(n).padStart(2, '0');
  return `${t.getFullYear()}-${p(t.getMonth() + 1)}-${p(t.getDate())} ${p(t.getHours())}:${p(t.getMinutes())}:${p(t.getSeconds())}`;
};

/** Build the 24h hourly history (most-recent first) for one device×point pair. */
const history = (deviceId: string, point: Record<string, unknown>): Record<string, unknown>[] => {
  const now = Date.now();
  return Array.from({length: 24}, (_, i) => {
    const ts = stamp(now - i * 3_600_000);
    const v = valueAt(point, i);
    return {
      id: `${deviceId}-${point.id}-${i}`,
      deviceId,
      pointId: point.id,
      rawValue: String(v),
      calValue: String(v),
      hasLatestValue: i === 0,
      rwFlag: point.rwFlag ?? 'READ_ONLY',
      createTime: ts,
      operateTime: ts,
    };
  });
};

/** Points belong to a device via the device's profile. */
const pointsOf = (deviceId: string): Record<string, unknown>[] => {
  const device = db.devices.find((d) => String(d.id) === String(deviceId));
  if (!device) return [];
  return db.points.filter((p) => String(p.profileId) === String(device.profileId));
};

/** Flatten history across devices (and points), optionally narrowed. */
const allRows = (deviceId?: string, pointId?: string): Record<string, unknown>[] => {
  const devices = deviceId ? db.devices.filter((d) => String(d.id) === String(deviceId)) : db.devices;
  const rows: Record<string, unknown>[] = [];
  for (const device of devices) {
    for (const point of pointsOf(String(device.id))) {
      if (pointId && String(point.id) !== String(pointId)) continue;
      rows.push(...history(String(device.id), point));
    }
  }
  return rows;
};

const cursorScope = (deviceId?: unknown, pointId?: unknown): string =>
  `${deviceId == null ? '' : String(deviceId)}|${pointId == null ? '' : String(pointId)}`;

const encodeCursor = (scope: string, offset: number): string =>
  Buffer.from(JSON.stringify({v: 1, scope, offset}), 'utf8').toString('base64url');

const decodeCursor = (token: unknown, scope: string): number => {
  if (typeof token !== 'string' || token.length === 0) return 0;
  try {
    const decoded = JSON.parse(Buffer.from(token, 'base64url').toString('utf8')) as {
      v?: number;
      scope?: string;
      offset?: number;
    };
    const offset = decoded.offset as number | undefined;
    if (decoded.v !== 1 || decoded.scope !== scope || typeof offset !== 'number' || !Number.isSafeInteger(offset)
      || offset < 0) throw new Error('invalid cursor');
    return offset as number;
  } catch {
    throw new Error('invalid cursor');
  }
};

/**
 * Realistic point-value time series so the value tables/charts render a live
 * 24h waveform instead of an empty state. Values are deterministic per point id
 * (stable across reloads) but anchored to the current hour so the demo feels live.
 */
export function registerTimeseriesHandlers(): void {
  // Latest value per device×point (only the most recent sample).
  on('post', 'api/v3/data/point_value/latest', (ctx) => {
    const rows = allRows(ctx.body?.deviceId, ctx.body?.pointId).filter((r) => r.hasLatestValue);
    return responseOf(ctx.config, ok(paginate(rows, ctx.body)));
  });

  // Paged history, optionally narrowed by device/point.
  on('post', 'api/v3/data/point_value/list', (ctx) => {
    const rows = allRows(ctx.body?.deviceId, ctx.body?.pointId);
    const limit = Math.max(1, Math.min(Number(ctx.body?.limit ?? 12), 200));
    const scope = cursorScope(ctx.body?.deviceId, ctx.body?.pointId);
    let offset: number;
    try {
      offset = decodeCursor(ctx.body?.cursor, scope);
    } catch {
      return responseOf(ctx.config, fail('INVALID_CURSOR', 'cursor is invalid or does not match this query'), 400);
    }
    const items = rows.slice(offset, offset + limit);
    const nextOffset = offset + items.length;
    const hasNext = nextOffset < rows.length;
    return responseOf(ctx.config, ok({
      items,
      nextCursor: hasNext ? encodeCursor(scope, nextOffset) : null,
      hasNext,
    }));
  });

  // GET history by device+point (query params use snake_case ids).
  on('get', 'api/v3/data/point_value/history', (ctx) => {
    const rows = allRows(ctx.params.device_id as string, ctx.params.point_id as string);
    const limit = Math.max(1, Math.min(Number(ctx.params.limit ?? 100), 500));
    const scope = cursorScope(ctx.params.device_id, ctx.params.point_id);
    let offset: number;
    try {
      offset = decodeCursor(ctx.params.cursor, scope);
    } catch {
      return responseOf(ctx.config, fail('INVALID_CURSOR', 'cursor is invalid or does not match this query'), 400);
    }
    const page = rows.slice(offset, offset + limit);
    const nextOffset = offset + page.length;
    const hasNext = nextOffset < rows.length;
    return responseOf(ctx.config, ok({
      items: page,
      nextCursor: hasNext ? encodeCursor(scope, nextOffset) : null,
      hasNext,
    }));
  });

  // Point commands are asynchronous: return the accepted command resource.
  on('post', 'api/v3/data/point_command/read', (ctx) => {
    const commandId = `mock-read-${ctx.body?.deviceId ?? 'device'}-${ctx.body?.pointId ?? 'point'}`;
    return responseOf(ctx.config, ok({
      commandId,
      statusUri: `/api/v3/data/point_command_history/get_by_command_id?commandId=${commandId}`,
    }), 202);
  });
  on('post', 'api/v3/data/point_command/write', (ctx) => {
    const commandId = `mock-write-${ctx.body?.deviceId ?? 'device'}-${ctx.body?.pointId ?? 'point'}`;
    return responseOf(ctx.config, ok({
      commandId,
      statusUri: `/api/v3/data/point_command_history/get_by_command_id?commandId=${commandId}`,
    }), 202);
  });
}
