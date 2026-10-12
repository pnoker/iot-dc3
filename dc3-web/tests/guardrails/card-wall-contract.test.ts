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

// Guardrail: the entity card wall keeps its auto-fill minmax shape and its
// gutter token. The runtime page-size alignment (useCardWallPageSize) reads
// the browser-resolved column count of THIS rule; a restyle to fixed
// breakpoint columns, or a floor change, silently desyncs that measurement
// and the last-row gap defect returns. The 380px floor lives only here —
// it is deliberately not duplicated in TypeScript.

import {readFileSync} from 'node:fs';
import {join} from 'node:path';

import {describe, expect, it} from 'vitest';

const root = process.cwd();

describe('card wall geometry contract (A5)', () => {
  const globalScss = readFileSync(join(root, 'src/styles/global.scss'), 'utf8');
  const wallBlock = globalScss.match(
    /\.entity-card-wall\s*\{[\s\S]*?\n\}/,
  )?.[0] ?? '';

  it('keeps the auto-fill minmax track shape', () => {
    expect(wallBlock).toContain('display: grid');
    expect(wallBlock).toMatch(
      /grid-template-columns:\s*repeat\(auto-fill,\s*minmax\(min\(\d+px,\s*100%\),\s*1fr\)\)/,
    );
  });

  it('keeps the shared gutter token and no fixed column rungs', () => {
    expect(wallBlock).toContain('gap: var(--dc3-gutter)');
    // Fixed breakpoint rungs (the pre-fluid lg/xl design) are exactly what
    // the runtime measurement cannot track.
    expect(wallBlock).not.toMatch(/grid-template-columns:\s*\d/);
    expect(wallBlock).not.toMatch(/@include|breakpoint/);
  });

  it('keeps the decorative filler and status blocks wall-scoped', () => {
    expect(wallBlock).toContain('.entity-card-wall__ghost');
    expect(wallBlock).toContain('> .el-empty');
  });
});

describe('wall page size constants (A5)', () => {
  const constants = readFileSync(join(root, 'src/config/constant/cardWall.ts'), 'utf8');

  it('mirrors the backend limit cap', () => {
    // dc3-db PageRequest.MAX_LIMIT and the mock readPage cap must agree.
    expect(constants).toMatch(/CARD_WALL_PAGE_SIZE_MAX\s*=\s*200/);
  });

  it('targets 12 records per page', () => {
    expect(constants).toMatch(/CARD_WALL_TARGET_PAGE_SIZE\s*=\s*12/);
  });
});
