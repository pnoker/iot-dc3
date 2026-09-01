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
import {newId, registerCrud, stamp} from '../crud';
import {db} from '../db';

/** Attribute-config rows scoped to one of the four owner kinds. */
const configScope = (
  rows: Record<string, unknown>[],
  deviceId: unknown,
  owner: 'driver' | 'point' | 'command' | 'event',
  ownerId?: unknown,
): Record<string, unknown>[] => {
  const has = (r: Record<string, unknown>, key: string) => {
    const v = r[key];
    return v !== undefined && v !== null && v !== '';
  };
  return rows.filter((r) => {
    if (String(r.deviceId) !== String(deviceId)) return false;
    if (owner === 'driver') return !has(r, 'pointId') && !has(r, 'commandId') && !has(r, 'eventId');
    if (owner === 'point') return has(r, 'pointId') && (!ownerId || String(r.pointId) === String(ownerId));
    if (owner === 'command') return has(r, 'commandId') && (!ownerId || String(r.commandId) === String(ownerId));
    return has(r, 'eventId') && (!ownerId || String(r.eventId) === String(ownerId));
  });
};

export function registerBusinessHandlers(): void {
  const registerVersionedCrud = (
    baseUrl: string,
    collection: 'commands' | 'commandParams' | 'events' | 'eventParams',
    search: string[] = [],
    exact: string[] = ['enableFlag'],
  ) => {
    const rows = () => db[collection];
    const matchesFilter = (row: Record<string, unknown>, body: Record<string, unknown>) =>
      search.every((field) => String(row[field] ?? '').toLowerCase().includes(String(body[field] ?? '').toLowerCase())) &&
      exact.every((field) => {
        const value = body[field];
        return value === undefined || value === null || value === '' || value === 'ALL' || String(row[field]) === String(value);
      });

    on('post', `${baseUrl}/list`, (ctx) => responseOf(ctx.config, ok(paginate(rows(), ctx.body, matchesFilter))));
    on('get', `${baseUrl}/get_by_id`, (ctx) =>
      responseOf(ctx.config, ok(rows().find((row) => String(row.id) === String(ctx.params.id)) ?? {})),
    );
    on('post', `${baseUrl}/add`, (ctx) => {
      const row = {...ctx.body, id: newId(), version: 0, createTime: stamp(), operateTime: stamp()};
      rows().push(row);
      return responseOf(ctx.config, ok(row), 201);
    });
    on('post', `${baseUrl}/update`, (ctx) => {
      const index = rows().findIndex(
        (row) => String(row.id) === String(ctx.body?.id) && Number(row.version) === Number(ctx.body?.version),
      );
      if (index < 0) return responseOf(ctx.config, fail('R4091', 'Version conflict', 409), 409);
      const row = {...rows()[index], ...ctx.body, version: Number(rows()[index]?.version ?? 0) + 1, operateTime: stamp()};
      rows()[index] = row;
      return responseOf(ctx.config, ok(row));
    });
    on('delete', `${baseUrl}/delete`, (ctx) => {
      const index = rows().findIndex(
        (row) => String(row.id) === String(ctx.params.id) && Number(row.version) === Number(ctx.params.version),
      );
      if (index < 0) return responseOf(ctx.config, fail('R4091', 'Version conflict', 409), 409);
      rows().splice(index, 1);
      return responseOf(ctx.config, null, 204);
    });
  };

  // ── A1. command ──
  registerVersionedCrud('api/v3/manager/command', 'commands', ['commandName', 'commandCode']);
  on('get', 'api/v3/manager/command/list_by_profile_id', (ctx) =>
    responseOf(
      ctx.config,
      ok(db.commands.filter((c) => String(c.profileId) === String(ctx.params.profile_id))),
    ),
  );

  // ── A2. command_param ──
  registerVersionedCrud('api/v3/manager/command_param', 'commandParams', [], ['commandId']);
  on('get', 'api/v3/manager/command_param/list_by_command_id', (ctx) =>
    responseOf(
      ctx.config,
      ok(db.commandParams.filter((p) => String(p.commandId) === String(ctx.params.command_id))),
    ),
  );

  // ── A3. event ──
  registerVersionedCrud('api/v3/manager/event', 'events', ['eventName', 'eventCode']);
  on('get', 'api/v3/manager/event/list_by_profile_id', (ctx) =>
    responseOf(
      ctx.config,
      ok(db.events.filter((e) => String(e.profileId) === String(ctx.params.profile_id))),
    ),
  );

  // ── A4. event_param ──
  registerVersionedCrud('api/v3/manager/event_param', 'eventParams', [], ['eventId']);
  on('get', 'api/v3/manager/event_param/list_by_event_id', (ctx) =>
    responseOf(
      ctx.config,
      ok(db.eventParams.filter((p) => String(p.eventId) === String(ctx.params.event_id))),
    ),
  );

  // ── A5. attribute metadata (shared across every driver) ──
  on('get', 'api/v3/manager/driver_attribute/list_by_driver_id', (ctx) => responseOf(ctx.config, ok(db.attributes)));
  on('get', 'api/v3/manager/point_attribute/list_by_driver_id', (ctx) => responseOf(ctx.config, ok(db.attributes)));
  on('get', 'api/v3/manager/command_attribute/list_by_driver_id', (ctx) => responseOf(ctx.config, ok(db.attributes)));
  on('get', 'api/v3/manager/event_attribute/list_by_driver_id', (ctx) => responseOf(ctx.config, ok(db.attributes)));

  // ── A6. attribute_config (driver/point/command/event scopes) ──
  on('get', 'api/v3/manager/driver_attribute_config/list_by_device_id', (ctx) =>
    responseOf(ctx.config, ok(configScope(db.attributeConfigs, ctx.params.device_id, 'driver'))),
  );
  on('get', 'api/v3/manager/point_attribute_config/list_by_device_id', (ctx) =>
    responseOf(ctx.config, ok(configScope(db.attributeConfigs, ctx.params.device_id, 'point'))),
  );
  on('get', 'api/v3/manager/point_attribute_config/list_by_device_id_and_point_id', (ctx) =>
    responseOf(ctx.config, ok(configScope(db.attributeConfigs, ctx.params.device_id, 'point', ctx.params.point_id))),
  );
  on('get', 'api/v3/manager/command_attribute_config/list_by_device_id', (ctx) =>
    responseOf(ctx.config, ok(configScope(db.attributeConfigs, ctx.params.device_id, 'command'))),
  );
  on('get', 'api/v3/manager/command_attribute_config/list_by_device_id_and_command_id', (ctx) =>
    responseOf(
      ctx.config,
      ok(configScope(db.attributeConfigs, ctx.params.device_id, 'command', ctx.params.command_id)),
    ),
  );
  on('get', 'api/v3/manager/event_attribute_config/list_by_device_id', (ctx) =>
    responseOf(ctx.config, ok(configScope(db.attributeConfigs, ctx.params.device_id, 'event'))),
  );
  on('get', 'api/v3/manager/event_attribute_config/list_by_device_id_and_event_id', (ctx) =>
    responseOf(ctx.config, ok(configScope(db.attributeConfigs, ctx.params.device_id, 'event', ctx.params.event_id))),
  );
  const registerAttributeConfigWrites = (baseUrl: string) => {
    on('post', `${baseUrl}/add`, (ctx) => {
      const row: Record<string, unknown> = {...ctx.body, id: newId(), version: 0};
      db.attributeConfigs.push(row);
      return responseOf(ctx.config, ok(row), 201);
    });
    on('patch', `${baseUrl}/update`, (ctx) => {
      const index = db.attributeConfigs.findIndex(
        (row) => String(row.id) === String(ctx.body?.id) && Number(row.version) === Number(ctx.body?.version),
      );
      if (index < 0) return responseOf(ctx.config, fail('R4091', 'Version conflict', 409), 409);
      const row = {
        ...db.attributeConfigs[index],
        ...ctx.body,
        version: Number(db.attributeConfigs[index]?.version ?? 0) + 1,
      };
      db.attributeConfigs[index] = row;
      return responseOf(ctx.config, ok(row));
    });
    on('delete', `${baseUrl}/delete`, (ctx) => {
      const index = db.attributeConfigs.findIndex(
        (row) => String(row.id) === String(ctx.params.id) && Number(row.version) === Number(ctx.params.version),
      );
      if (index < 0) return responseOf(ctx.config, fail('R4091', 'Version conflict', 409), 409);
      db.attributeConfigs.splice(index, 1);
      return responseOf(ctx.config, null, 204);
    });
  };
  registerAttributeConfigWrites('api/v3/manager/driver_attribute_config');
  registerAttributeConfigWrites('api/v3/manager/point_attribute_config');
  registerAttributeConfigWrites('api/v3/manager/command_attribute_config');
  registerAttributeConfigWrites('api/v3/manager/event_attribute_config');

  // ── B. alarm sub-resources ──
  registerCrud({baseUrl: 'api/v3/data/rule', collection: 'alarmRules', search: ['ruleName', 'ruleCode']});
  registerCrud({baseUrl: 'api/v3/data/notify', collection: 'alarmNotifies', search: ['notifyName']});
  registerCrud({baseUrl: 'api/v3/data/message', collection: 'alarmMessages', search: ['messageName']});
  registerCrud({baseUrl: 'api/v3/data/notify/channel', collection: 'alarmChannels', search: ['channelName']});
  registerCrud({
    baseUrl: 'api/v3/data/notify/channel/bind',
    collection: 'alarmChannelBinds',
    exact: ['notifyId', 'channelId'],
  });
  registerCrud({baseUrl: 'api/v3/data/rule/state', collection: 'alarmRuleStates', exact: ['ruleId']});
  registerCrud({baseUrl: 'api/v3/data/notify/history', collection: 'alarmHistories'});

  // ── C. command / event history (synthesized from the seed definitions) ──
  on('post', 'api/v3/manager/command_history/list', (ctx) => {
    const deviceId = db.devices[0]?.id ?? '1';
    const rows = db.commands.slice(0, 3).map((c) => ({
      recordId: newId(),
      deviceId,
      commandId: c.id,
      commandName: c.commandName,
      commandCode: c.commandCode,
      paramValues: {},
      status: 'SUCCESS',
      occurTime: stamp(),
    }));
    return responseOf(ctx.config, ok(paginate(rows, ctx.body)));
  });
  on('post', 'api/v3/data/event_history/list', (ctx) => {
    const deviceId = db.devices[0]?.id ?? '1';
    const rows = db.events.slice(0, 3).map((e) => ({
      recordId: newId(),
      deviceId,
      eventId: e.id,
      eventName: e.eventName,
      eventCode: e.eventCode,
      paramValues: {},
      message: e.remark,
      occurTime: stamp(),
    }));
    return responseOf(ctx.config, ok(paginate(rows, ctx.body)));
  });
}
