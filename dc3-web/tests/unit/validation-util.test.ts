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

import {isEmail, isNull, isNum, isPhone, isUrl} from '@/utils/validationUtil';

describe('validationUtil', () => {
  describe('isUrl', () => {
    it.each([
      ['http://example.com', true],
      ['https://dc3.example.com/path', true],
      ['HTTPS://Example.COM', false], // case-sensitive http(s) prefix
      ['ftp://example.com', false],
      ['javascript:alert(1)', false],
      ['', false],
      ['just-text', false],
      [null, false],
      [undefined, false],
      [42, false],
    ])('isUrl(%p) → %p', (input, expected) => {
      expect(isUrl(input)).toBe(expected);
    });
  });

  describe('isEmail', () => {
    it.each([
      ['dc3@example.com', true],
      ['user.name+tag@example.co.uk', false], // '+' not allowed by current regex
      ['user.name@example.co.uk', true],
      ['a@b.cd', true],
      ['plain', false],
      ['@example.com', false],
      ['user@', false],
      ['user@example', false],
      // The TLD piece is `([a-zA-Z0-9]{2,4})+$` — the trailing `+` quantifier
      // means a 10-char TLD can still match as 4+4+2. Pin this current
      // behaviour so a future regex tightening shows up in this test.
      ['user@example.toolongtld', true],
      ['', false],
      [null, false],
      [undefined, false],
    ])('isEmail(%p) → %p', (input, expected) => {
      expect(isEmail(input)).toBe(expected);
    });
  });

  describe('isPhone', () => {
    it.each([
      ['13912345678', true],
      ['18512345678', true],
      ['14712345678', true],
      ['16612345678', true], // 16x prefix (China Unicom IoT)
      ['17812345678', true], // 17x prefix (virtual operator)
      ['19912345678', true], // 19x prefix (China Telecom)
      ['10000000000', false], // invalid prefix (10x)
      ['12000000000', false], // invalid prefix (12x)
      ['1391234567', false], // too short
      ['139123456789', false], // too long
      ['abcdefghijk', false],
      ['', false],
      [null, false],
      [undefined, false],
    ])('isPhone(%p) → %p', (input, expected) => {
      expect(isPhone(input)).toBe(expected);
    });
  });

  describe('isNum', () => {
    // type=1: decimal-aware (digits and optional dot)
    // type=2: integer-only (pure digits)
    // default: always true (backward-compatible fallthrough)
    it.each([
      // type=1 (decimal)
      ['123', 1, true],
      ['1.5', 1, true],
      ['.5', 1, true],
      ['0', 1, true],
      ['abc', 1, false],
      ['12a', 1, false],
      // type=2 (integer)
      ['5', 2, true],
      ['123', 2, true],
      ['1.5', 2, false],
      ['a5', 2, false],
      // unknown type — fallthrough to true
      ['123', 0, true],
      ['abc', 99, true],
    ])('isNum(%p, %i) → %p', (input, type, expected) => {
      expect(isNum(input, type)).toBe(expected);
    });
  });

  describe('isNull', () => {
    it.each([
      [undefined, true],
      [null, true],
      ['', true],
      ['null', true],
      ['undefined', true],
      ['ok', false],
      [0, false], // numbers are never null
      [-1, false],
      [Number.NaN, false],
      [false, false], // booleans are never null
      [true, false],
      [[], true],
      [[1], false],
      [{}, true],
      [{a: 1}, false],
    ])('isNull(%p) → %p', (input, expected) => {
      expect(isNull(input)).toBe(expected);
    });

    it('treats nested empty arrays/objects as non-null', () => {
      expect(isNull([[]])).toBe(false);
      expect(isNull({nested: {}})).toBe(false);
    });
  });
});
