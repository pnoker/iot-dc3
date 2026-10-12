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

// Guardrail: the hardcoded-page-size defect class cannot come back. The
// card walls size themselves from the measured column count (autoPageSize
// on usePagedList / useCardWallAuto), and skeletons track page.size — a new
// `pageSize: <literal>` option or a fixed skeleton count reintroduces the
// ragged-last-row bug on the very next width band. Fixed sizes remain legal
// only where the surface is a table, not a card wall (whitelist below).

import {readdirSync, readFileSync, statSync} from 'node:fs';
import {join, relative} from 'node:path';

import {describe, expect, it} from 'vitest';

const root = process.cwd();
// src/components/card is in scope: SkeletonCard's fixed 12-card grid lived
// there, so the exact regression site must stay scanned. Table-list loading
// placeholders outside the card domain (EntityListPage, ResponsiveRecordList)
// are not wall surfaces and keep their fixed shimmer counts.
const scanDirs = [join(root, 'src/views'), join(root, 'src/composables'), join(root, 'src/components/card')];

// Intentional fixed sizes: table surfaces whose rows need no wall
// alignment. Everything else must go through autoPageSize / page.size.
const WHITELIST = [
  'src/views/settings/identityAudit/identityAuditConfig.ts', // audit table, pageSize 20
  'src/views/device/edit/DeviceEdit.vue', // wizard matrix tables, page-sizes [10, 20, 50]
];

const PAGE_SIZE_LITERAL = /pageSize:\s*\d+/;
const SKELETON_COUNT_LITERAL = /v-for="[^"]*\bin\s+\d+"/;

function walk(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) return walk(path);
    return /\.(vue|ts)$/.test(path) ? [path] : [];
  });
}

function findViolations(): string[] {
  const violations: string[] = [];
  for (const dir of scanDirs) {
    for (const path of walk(dir)) {
      const rel = relative(root, path).replace(/\\/g, '/');
      if (WHITELIST.includes(rel)) continue;
      const content = readFileSync(path, 'utf8');
      content.split('\n').forEach((line, index) => {
        if (PAGE_SIZE_LITERAL.test(line)) {
          violations.push(
            `${rel}:${index + 1} — hardcoded pageSize literal; card lists use autoPageSize (wall-managed) or the useEntityListPage table config`,
          );
        }
        if (SKELETON_COUNT_LITERAL.test(line) && /skeleton/i.test(line)) {
          violations.push(
            `${rel}:${index + 1} — fixed skeleton count; bind v-for to Math.max(1, page.size) so the loading wall previews the incoming fill`,
          );
        }
      });
    }
  }
  return violations;
}

describe('card wall page size guard (A5)', () => {
  it('rejects hardcoded page sizes and fixed skeleton counts outside the table whitelist', () => {
    expect(findViolations()).toEqual([]);
  });

  it('keeps the whitelist entries meaningful', () => {
    for (const rel of WHITELIST) {
      expect(() => readFileSync(join(root, rel), 'utf8')).not.toThrow();
    }
  });
});
