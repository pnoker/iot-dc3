/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {listDeviceByIds} from '@/api/device';
import {listPrincipal, listPrincipalByIds} from '@/api/principal';
import {listRole} from '@/api/role';
import type {EntityFieldConfig, EntityRelation} from '@/config/types/entityList';

// Shared `config.relations` loaders for settings lists — turn entity-id foreign
// keys into display names through the family relations mechanism (same pattern
// as groupConfig.ts / EventTable.vue). Each factory reads an id prop off the
// rows, batch-resolves names, and returns an {id: name} map for ctx.relations[key].

const collectIds = (rows: Record<string, any>[], prop: string): string[] => [
  ...new Set(rows.map((r) => String(r[prop] ?? '')).filter((id) => id && id !== '0')),
];

/**
 * principalId → displayName || principalName. Reused by audit / tenant-membership /
 * local-credential / role-binding / service-account (ownerPrincipalId) / mcp lists.
 * Pass a different `prop`/`key` for non-default columns (e.g. ownerPrincipalId).
 */
export const principalNameRelation = (prop = 'principalId', key = 'principalName'): EntityRelation => ({
  key,
  load: async (rows) => {
    const ids = collectIds(rows, prop);
    if (!ids.length) return {};
    const res: any = await listPrincipalByIds(ids);
    const map: Record<string, string> = {};
    (res?.data || []).forEach((p: any) => {
      map[String(p.id)] = p.displayName || p.principalName || String(p.id);
    });
    return map;
  },
});

/** deviceId → deviceName. listDeviceByIds returns an {id: record} map (see EventTable). */
export const deviceNameRelation = (prop = 'deviceId', key = 'deviceName'): EntityRelation => ({
  key,
  load: async (rows) => {
    const ids = collectIds(rows, prop);
    if (!ids.length) return {};
    const res: any = await listDeviceByIds(ids);
    const data = res?.data || {};
    const map: Record<string, string> = {};
    ids.forEach((id) => {
      if (data[id]) map[id] = data[id].deviceName || id;
    });
    return map;
  },
});

/** roleId → roleName. Roles are few, so a single full-page list builds the map. */
export const roleNameRelation = (prop = 'roleId', key = 'roleName'): EntityRelation => ({
  key,
  load: async (rows) => {
    const ids = collectIds(rows, prop);
    if (!ids.length) return {};
    const res: any = await listRole({page: {current: 1, size: 1000}});
    const records = res?.data?.records || [];
    const map: Record<string, string> = {};
    records.forEach((r: any) => {
      map[String(r.id)] = r.roleName || String(r.id);
    });
    return map;
  },
});

/**
 * Add/edit form field for choosing a principal by name (dropdown) instead of typing a
 * raw id. Loads the principal roster and labels each option with displayName / principalName.
 */
export const principalIdField = (label: string, prop = 'principalId'): EntityFieldConfig => ({
  prop,
  label,
  kind: 'treeSelect',
  tree: {
    load: async () => {
      const res: any = await listPrincipal({page: {current: 1, size: 1000}});
      return (res?.data?.records || []).map((p: any) => ({
        ...p,
        displayLabel: p.displayName || p.principalName || String(p.id),
      }));
    },
    props: {label: 'displayLabel', value: 'id'},
  },
  required: true,
});
