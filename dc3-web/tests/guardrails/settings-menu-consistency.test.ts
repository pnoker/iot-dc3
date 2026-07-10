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

import {existsSync, readFileSync} from 'node:fs';
import {join} from 'node:path';

import {describe, expect, it} from 'vitest';

import {SETTINGS_FALLBACK_SIDEBAR} from '@/config/settingsNav';

// The settings sidebar is authoritatively driven by the backend dc3_menu seed,
// with settingsNav.ts as the offline fallback. They MUST encode the same group
// tree — when they drift, users see a different (often messier) menu than the
// code suggests. This gate parses the seed SQL and asserts its settings subtree
// matches SETTINGS_FALLBACK_SIDEBAR group-for-group, leaf-for-leaf, in order.
const SEED = join(process.cwd(), '../iot-dc3/dc3/dependencies/postgres/initdb/02-iot-dc3-auth.sql');

interface MenuRow {
  id: number;
  parent: number;
  code: string;
  index: number;
}

function parseSeedMenu(sql: string): MenuRow[] {
  // menu tuple: (id, parent, type, 'name', 'code', level, index, ...) — three
  // leading integers distinguish it from resource tuples (id, parent, 'name').
  const re = /\((\d+),\s*(\d+),\s*\d+,\s*'[^']*',\s*'([^']*)',\s*\d+,\s*(\d+),/g;
  const rows: MenuRow[] = [];
  for (let m; (m = re.exec(sql));) {
    const id = Number(m[1]);
    if (id < 10001 || id > 10099) continue; // menu id band
    rows.push({id, parent: Number(m[2]), code: m[3], index: Number(m[4])});
  }
  return rows;
}

type Tree = Array<{ code: string; leaves: string[] }>;

const sortByIndex = (a: MenuRow, b: MenuRow) => a.index - b.index;

describe('settings menu — backend seed ↔ frontend nav', () => {
  it.skipIf(!existsSync(SEED))('encodes the same 8-group tree in seed SQL and settingsNav.ts', () => {
    const rows = parseSeedMenu(readFileSync(SEED, 'utf8'));
    const settings = rows.find((r) => r.code === 'settings');
    expect(settings, 'settings root menu present in seed').toBeTruthy();

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
  });
});
