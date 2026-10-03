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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

// Guardrail: the en/zh locale catalogs must stay in key parity. A key added
// to one catalog but not the other renders the raw key (or a wrong-language
// fallback) in the UI. Both catalogs are plain default-export object
// literals under src/config/i18n/locales; this test extracts every leaf key
// path recursively and diffs both directions. Values (strings, arrays,
// interpolation placeholders) are intentionally not compared — only presence.

import { describe, expect, it } from 'vitest';

import en from '@/config/i18n/locales/en';
import zh from '@/config/i18n/locales/zh';

type Dictionary = Record<string, unknown>;

function isDictionary(value: unknown): value is Dictionary {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function leafPaths(dictionary: Dictionary, prefix = ''): string[] {
  const paths: string[] = [];
  for (const [key, value] of Object.entries(dictionary)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (isDictionary(value)) {
      paths.push(...leafPaths(value, path));
    } else {
      paths.push(path);
    }
  }
  return paths;
}

const enPaths = new Set(leafPaths(en as Dictionary));
const zhPaths = new Set(leafPaths(zh as Dictionary));

const MAX_REPORTED = 50;

describe('i18n locale parity', () => {
  it('zh defines every leaf key that en defines', () => {
    const missing = [...enPaths].filter((path) => !zhPaths.has(path)).sort();
    expect(
      missing.slice(0, MAX_REPORTED).join('\n'),
      `zh is missing ${missing.length} en keys (first ${Math.min(missing.length, MAX_REPORTED)} shown)`
    ).toEqual('');
  });

  it('en defines every leaf key that zh defines', () => {
    const missing = [...zhPaths].filter((path) => !enPaths.has(path)).sort();
    expect(
      missing.slice(0, MAX_REPORTED).join('\n'),
      `en is missing ${missing.length} zh keys (first ${Math.min(missing.length, MAX_REPORTED)} shown)`
    ).toEqual('');
  });
});
