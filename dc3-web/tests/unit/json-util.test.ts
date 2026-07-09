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

import {describe, expect, it} from 'vitest';

import {prettyJson} from '@/utils/jsonUtil';

describe('jsonUtil', () => {
  it('returns the fallback for null, undefined, and empty string', () => {
    expect(prettyJson(null)).toBe('-');
    expect(prettyJson(undefined)).toBe('-');
    expect(prettyJson('')).toBe('-');
    expect(prettyJson(null, 'N/A')).toBe('N/A');
  });

  it('reformats parseable JSON strings with two-space indentation', () => {
    expect(prettyJson('{"a":1,"b":[2,3]}')).toBe('{\n  "a": 1,\n  "b": [\n    2,\n    3\n  ]\n}');
  });

  it('returns the original string when it is not valid JSON', () => {
    expect(prettyJson('not-json')).toBe('not-json');
    expect(prettyJson('{ unclosed')).toBe('{ unclosed');
  });

  it('serializes plain objects and arrays', () => {
    expect(prettyJson({name: 'driver', enabled: true})).toBe('{\n  "name": "driver",\n  "enabled": true\n}');
    expect(prettyJson([1, 2])).toBe('[\n  1,\n  2\n]');
  });
});
