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

import * as ElementIcons from '@element-plus/icons-vue';
import {describe, expect, it} from 'vitest';

import {SETTINGS_FALLBACK_ICON, SETTINGS_FALLBACK_SIDEBAR} from '@/config/settingsNav';
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
  icon?: string;
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
  // The optional 7th capture is the menu_ext JSON string (no single quotes
  // inside — its inner JSON escapes double quotes only), carried on the next
  // line after menu_index.
  const re = /\((\d+),\s*(\d+),\s*\d+,\s*'[^']*',\s*'([^']*)',\s*(\d+),\s*(\d+),\s*'(\{[^']*\})'/g;
  const rows: MenuRow[] = [];
  for (let m; (m = re.exec(sql));) {
    const id = Number(m[1]);
    if (id < 10001 || id > 10099) continue; // menu id band
    let icon: string | undefined;
    try {
      // menu_ext.content is itself a stringified JSON doc: {titles, icon, url}.
      icon = JSON.parse(JSON.parse(m[6]).content).icon;
    } catch {
      icon = undefined;
    }
    rows.push({id, parent: Number(m[2]), code: m[3], level: Number(m[4]), index: Number(m[5]), icon});
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

  // Icon contract (A5 single source of truth, applied to the seed SQL,
  // settingsNav fallback and the static mock). The icon mapping lives in
  // exactly one table: docs/design + this test enforce that all three
  // copies stay identical, that every icon exists in the registered
  // Element Plus icon pool, and that the mapping stays semantically
  // legible: unique per group, no parent/child duplication. Deliberately
  // tolerated exceptions (cross-domain mirrors): alarm leaves mirror
  // their source entity icons (Promotion/Management/TrendCharts), and
  // "bind/connect" leaves may share Link.
  it('keeps settings icons identical across seed SQL, settingsNav and mock, and semantically unique per group', () => {
    const sql = readFileSync(SEED, 'utf8');
    const rows = parseSeedMenu(sql);
    const settingsRoot = rows.find((r) => r.code === 'settings');
    expect(settingsRoot, 'settings root menu present in seed').toBeDefined();

    // 1. Every settings menu row carries a parsable icon that exists in the
    //    registered Element Plus pool (a bad name renders as nothing).
    const settingsIds = new Set<number>([settingsRoot!.id]);
    let previousSize = -1;
    while (settingsIds.size !== previousSize) {
      previousSize = settingsIds.size;
      rows.filter((row) => settingsIds.has(row.parent)).forEach((row) => settingsIds.add(row.id));
    }
    const iconRows = rows.filter((row) => settingsIds.has(row.id));
    for (const row of iconRows) {
      expect(row.icon, `${row.code} has a parsable icon in seed SQL`).toBeDefined();
      expect(
        row.icon! in ElementIcons,
        `${row.code} icon "${row.icon}" is an exported @element-plus/icons-vue component`,
      ).toBe(true);
    }

    // 2. Seed ↔ settingsNav fallback equality for every settings code.
    for (const row of iconRows) {
      expect(
        SETTINGS_FALLBACK_ICON[row.code],
        `${row.code} present in SETTINGS_FALLBACK_ICON`,
      ).toBeDefined();
      expect(SETTINGS_FALLBACK_ICON[row.code], `${row.code} seed ↔ settingsNav icon equality`).toBe(row.icon);
    }

    // 3. Seed ↔ static mock equality (mock renders the same wall).
    const mockSettings = menuTree.find((row) => row.menuCode === 'settings');
    const mockRows = [mockSettings!, ...(mockSettings!.children ?? []).flatMap((group) => [group, ...(group.children ?? [])])];
    for (const mock of mockRows) {
      const seed = rows.find((row) => row.code === mock.menuCode);
      expect(seed, `${mock.menuCode} present in seed`).toBeDefined();
      expect(mock.menuExt?.content?.icon, `${mock.menuCode} mock ↔ seed icon equality`).toBe(seed!.icon);
    }

    // 4. Semantic legibility: leaves unique per group; group icon differs
    //    from every leaf icon in that group (exact names — a filled variant
    //    like UserFilled next to leaf User is a deliberate distinct glyph).
    const groups = iconRows.filter((row) => row.parent === settingsRoot!.id);
    for (const group of groups) {
      const leaves = iconRows.filter((row) => row.parent === group.id);
      const leafIcons = leaves.map((leaf) => leaf.icon!);
      expect(
        new Set(leafIcons).size,
        `${group.code} leaves have unique icons (got ${leafIcons.join(', ')})`,
      ).toBe(leafIcons.length);
      expect(
        leaves.every((leaf) => leaf.icon !== group.icon),
        `${group.code} group icon "${group.icon}" differs from its leaves`,
      ).toBe(true);
    }
    const groupIcons = groups.map((group) => group.icon!);
    expect(new Set(groupIcons).size, 'settings group icons are distinct').toBe(groupIcons.length);

    // 5. Detail-page fallback keys mirror their list sibling exactly.
    for (const code of Object.keys(SETTINGS_FALLBACK_ICON)) {
      if (!code.endsWith('Detail')) continue;
      const sibling = code.replace(/Detail$/, '');
      if (SETTINGS_FALLBACK_ICON[sibling]) {
        expect(SETTINGS_FALLBACK_ICON[code], `${code} mirrors ${sibling}`).toBe(SETTINGS_FALLBACK_ICON[sibling]);
      }
    }
  });
});
