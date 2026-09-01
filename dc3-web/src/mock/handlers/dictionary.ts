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

import type {Dictionary} from '@/config/types';

import {on} from '../dispatch';
import {localizeEntities} from '../locale';
import {matches, paginate} from '../query';
import {responseOf} from '../response';

const toDict = (row: Record<string, unknown>, label: unknown, type: string): Dictionary => ({
  type,
  label: String(label ?? row.id),
  value: String(row.id),
  disabled: false,
  expand: false,
  children: [],
});

const dictPage = (rows: Dictionary[], body: Record<string, unknown>) =>
  paginate(rows, body, (dictionary) => matches(dictionary.label, body.label));

export function registerDictionaryHandlers(): void {
  on('get', 'api/v3/auth/dictionary/list_tenant', (ctx) =>
    responseOf(ctx.config, [
      {
        label: 'Default Tenant',
        value: 'default',
        disabled: false,
        expand: false,
        children: [],
      } satisfies Dictionary,
    ]),
  );

  on('post', 'api/v3/manager/dictionary/list_driver', (ctx) => {
    const rows = localizeEntities('driver', ctx.db.drivers).map((d) => toDict(d, d.driverName, 'driver'));
    return responseOf(ctx.config, dictPage(rows, ctx.body));
  });

  on('post', 'api/v3/manager/dictionary/list_device', (ctx) => {
    const rows = localizeEntities('device', ctx.db.devices).map((d) => toDict(d, d.deviceName, 'device'));
    return responseOf(ctx.config, dictPage(rows, ctx.body));
  });

  on('post', 'api/v3/manager/dictionary/list_profile', (ctx) => {
    const rows = localizeEntities('profile', ctx.db.profiles).map((p) => toDict(p, p.profileName, 'profile'));
    return responseOf(ctx.config, dictPage(rows, ctx.body));
  });

  on('post', 'api/v3/manager/dictionary/list_device_point', (ctx) => {
    // parentId is a deviceId — narrow to that device's profile points.
    const parentId = ctx.body.parentId;
    let pool = ctx.db.points;
    if (parentId) {
      const device = ctx.db.devices.find((d) => String(d.id) === String(parentId));
      if (device?.profileId) {
        pool = ctx.db.points.filter((p) => String(p.profileId) === String(device.profileId));
      }
    }
    const rows = localizeEntities('point', pool).map((p) => toDict(p, p.pointName, 'point'));
    return responseOf(ctx.config, dictPage(rows, ctx.body));
  });

  on('post', 'api/v3/manager/dictionary/list_profile_point', (ctx) => {
    const pool = ctx.body.parentId
      ? ctx.db.points.filter((point) => String(point.profileId) === String(ctx.body.parentId))
      : ctx.db.points;
    const rows = localizeEntities('point', pool).map((point) => toDict(point, point.pointName, 'point'));
    return responseOf(ctx.config, dictPage(rows, ctx.body));
  });

  on('post', 'api/v3/manager/dictionary/list_driver_device', (ctx) => {
    const pool = ctx.body.parentId
      ? ctx.db.devices.filter((device) => String(device.driverId) === String(ctx.body.parentId))
      : ctx.db.devices;
    const rows = localizeEntities('device', pool).map((device) => toDict(device, device.deviceName, 'device'));
    return responseOf(ctx.config, dictPage(rows, ctx.body));
  });
}
