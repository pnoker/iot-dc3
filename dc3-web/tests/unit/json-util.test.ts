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
