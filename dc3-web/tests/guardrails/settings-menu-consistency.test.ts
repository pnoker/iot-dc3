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

import {readFileSync} from 'node:fs';
import {basename, resolve} from 'node:path';

import {describe, expect, it} from 'vitest';

import {SETTINGS_FALLBACK_SIDEBAR} from '@/config/settingsNav';
import {menuTree} from '@/mock/seed/menuTree';

// The settings sidebar is authoritatively driven by the backend dc3_menu seed,
// with settingsNav.ts as the offline fallback. They MUST encode the same group
// tree — when they drift, users see a different (often messier) menu than the
// code suggests. This gate parses the seed SQL and asserts its settings subtree
// matches SETTINGS_FALLBACK_SIDEBAR group-for-group, leaf-for-leaf, in order.
const REPOSITORY_ROOT = basename(process.cwd()) === 'dc3-web' ? resolve(process.cwd(), '..') : process.cwd();
const SEED = resolve(REPOSITORY_ROOT, 'dc3/dependencies/postgres/initdb/02-iot-dc3-auth.sql');

interface MenuRow {
  id: number;
  parent: number;
  code: string;
  level: number;
  index: number;
}

interface MenuResourceRow {
  id: number;
  parent: number;
  code: string;
  entityId: number;
}

function parseSeedMenu(sql: string): MenuRow[] {
  // menu tuple: (id, parent, type, 'name', 'code', level, index, ...) — three
  // leading integers distinguish it from resource tuples (id, parent, 'name').
  const re = /\((\d+),\s*(\d+),\s*\d+,\s*'[^']*',\s*'([^']*)',\s*(\d+),\s*(\d+),/g;
  const rows: MenuRow[] = [];
  for (let m; (m = re.exec(sql));) {
    const id = Number(m[1]);
    if (id < 10001 || id > 10099) continue; // menu id band
    rows.push({id, parent: Number(m[2]), code: m[3], level: Number(m[4]), index: Number(m[5])});
  }
  return rows;
}

function parseSeedMenuResources(sql: string): MenuResourceRow[] {
  const re = /\((2\d{4}),\s*(\d+),\s*'[^']*',\s*'menu:([^']+)',\s*'[^']*',\s*\d+,\s*\d+,\s*(\d+),/g;
  const rows: MenuResourceRow[] = [];
  for (let m; (m = re.exec(sql));) {
    rows.push({id: Number(m[1]), parent: Number(m[2]), code: m[3], entityId: Number(m[4])});
  }
  return rows;
}

type Tree = Array<{ code: string; leaves: string[] }>;

const sortByIndex = (a: MenuRow, b: MenuRow) => a.index - b.index;

describe('settings menu — backend seed ↔ frontend nav', () => {
  it('encodes the same settings tree in seed SQL and settingsNav.ts', () => {
    const sql = readFileSync(SEED, 'utf8');
    const rows = parseSeedMenu(sql);
    const settings = rows.find((r) => r.code === 'settings');
    expect(settings, 'settings root menu present in seed').toBeDefined();

    const seedTree: Tree = rows
      .filter((r) => r.parent === settings!.id)
      .sort(sortByIndex)
      .map((group) => ({
        code: group.code,
        leaves: rows
          .filter((r) => r.parent === group.id)
          .sort(sortByIndex)
          .map((r) => r.code),
      }));

    const navTree: Tree = SETTINGS_FALLBACK_SIDEBAR.map((group) => ({
      code: group.name,
      leaves: (group.children ?? []).map((child) => child.name),
    }));

    expect(seedTree).toEqual(navTree);

    const topLevel = rows.filter((r) => r.parent === settings!.id);
    expect(topLevel.every((row) => row.level === 2), 'settings children use menu level C2').toBe(true);
    const groupIds = new Set(topLevel.map((row) => row.id));
    expect(
      rows.filter((row) => groupIds.has(row.parent)).every((row) => row.level === 3),
      'settings group leaves use menu level C3',
    ).toBe(true);

    const settingsIds = new Set([settings!.id]);
    let previousSize = -1;
    while (settingsIds.size !== previousSize) {
      previousSize = settingsIds.size;
      rows.filter((row) => settingsIds.has(row.parent)).forEach((row) => settingsIds.add(row.id));
    }
    const resourcesByEntity = new Map(parseSeedMenuResources(sql).map((row) => [row.entityId, row]));
    for (const menu of rows.filter((row) => settingsIds.has(row.id))) {
      const resource = resourcesByEntity.get(menu.id);
      expect(resource, `MENU resource exists for ${menu.code}`).toBeDefined();
      expect(resource!.code).toBe(menu.code);
      const parentResource = menu.parent === 0 ? undefined : resourcesByEntity.get(menu.parent);
      expect(resource!.parent, `MENU resource parent mirrors ${menu.code}`).toBe(parentResource?.id ?? 0);
    }
  });

  it('keeps the static mock settings tree in the same order as settingsNav.ts', () => {
    const settings = menuTree.find((row) => row.menuCode === 'settings');
    expect(settings, 'settings root menu present in static mock').toBeDefined();

    const mockTree: Tree = (settings!.children ?? []).map((group) => ({
      code: group.menuCode,
      leaves: (group.children ?? []).map((child) => child.menuCode),
    }));
    const navTree: Tree = SETTINGS_FALLBACK_SIDEBAR.map((group) => ({
      code: group.name,
      leaves: (group.children ?? []).map((child) => child.name),
    }));

    expect(mockTree).toEqual(navTree);
  });
});
