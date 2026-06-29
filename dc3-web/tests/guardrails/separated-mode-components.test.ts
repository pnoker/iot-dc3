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

import {existsSync, readdirSync, readFileSync, statSync} from 'node:fs';
import {dirname, join, relative, resolve} from 'node:path';

import {describe, expect, it} from 'vitest';

// unplugin-vue-components auto-registers Element Plus (el-*) and every SFC under
// src/components — but only by INJECTING the import into the consuming SFC's
// inline <script>. A component using `<script src="./index.ts">` has no inline
// block to inject into, so any local component it references in the template
// MUST be imported + registered in that external .ts by hand. Forget, and the
// page throws "Failed to resolve component" the moment it opens (the Principal
// regression). This gate fails the build instead of the runtime.

const root = process.cwd();
const SRC = join(root, 'src');

// kebab tags that are framework/native, never a local SFC.
const NON_COMPONENT = new Set(['router-view', 'router-link', 'transition-group', 'keep-alive']);

const toKebab = (s: string): string =>
  s
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/([A-Z]+)([A-Z][a-z])/g, '$1-$2')
    .toLowerCase();
const toPascal = (kebab: string): string =>
  kebab
    .split('-')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('');

function walk(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const path = join(dir, entry);
    return statSync(path).isDirectory() ? walk(path) : [path];
  });
}

describe('separated-mode SFC component registration', () => {
  it('registers every local component its template uses', () => {
    const offenders: string[] = [];

    for (const vuePath of walk(SRC).filter((p) => p.endsWith('.vue'))) {
      const src = readFileSync(vuePath, 'utf8');
      const srcMatch = src.match(/<script[^>]*\bsrc=["'](\.[^"']+)["']/);
      if (!srcMatch) continue; // not separated-mode

      const tsPath = resolve(dirname(vuePath), srcMatch[1]);
      if (!existsSync(tsPath)) continue;
      const script = readFileSync(tsPath, 'utf8');

      const template = (src.match(/<template>([\s\S]*?)<\/template>/) || ['', ''])[1];
      const missing = new Set<string>();
      for (const tag of template.matchAll(/<([A-Za-z][\w-]*)[\s/>]/g)) {
        const kebab = toKebab(tag[1]);
        if (!kebab.includes('-') || kebab.startsWith('el-') || NON_COMPONENT.has(kebab)) continue;
        // The PascalCase name must appear somewhere in the external script —
        // as an import and/or a `components: {}` entry.
        if (!script.includes(toPascal(kebab))) missing.add(kebab);
      }
      if (missing.size) {
        offenders.push(`${relative(root, vuePath).replaceAll('\\', '/')} → ${[...missing].join(', ')}`);
      }
    }

    expect(offenders).toEqual([]);
  });
});
