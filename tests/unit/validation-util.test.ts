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

import { describe, expect, it } from 'vitest';

import { isEmail, isNull, isNum, isNumord, isPhone, isUrl } from '@/utils/validationUtil';

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
      ['10000000000', false], // invalid prefix
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
    // isNum returns true for empty string and pure numbers (the regex matches
    // strings starting with non-digit/dot characters, so the inverse defines
    // a "looks like number" check). These cases pin the actual behaviour so
    // refactors of the regex don't accidentally invert the semantics.
    it('returns false for type=1 when input starts with a digit or dot', () => {
      expect(isNum('123', 1)).toBe(false);
      expect(isNum('.5', 1)).toBe(false);
    });

    it('returns true for type=1 when input starts with a non-digit non-dot', () => {
      expect(isNum('abc', 1)).toBe(true);
    });

    it('returns false for type=2 when input starts with a digit', () => {
      expect(isNum('5', 2)).toBe(false);
    });

    it('returns true for type=2 when input starts with a non-digit', () => {
      expect(isNum('a5', 2)).toBe(true);
    });

    it('returns true for unknown type values without inspecting input', () => {
      // The function falls through to `return true` when type is neither 1 nor 2.
      expect(isNum('123', 0)).toBe(true);
      expect(isNum('abc', 99)).toBe(true);
    });
  });

  describe('isNumord', () => {
    it('treats both type=1 and type=2 the same way (decimal-aware regex)', () => {
      expect(isNumord('1.5', 1)).toBe(false);
      expect(isNumord('1.5', 2)).toBe(false);
      expect(isNumord('a', 1)).toBe(true);
      expect(isNumord('a', 2)).toBe(true);
    });

    it('falls through to true for unknown types', () => {
      expect(isNumord('1.5', 99)).toBe(true);
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
      [{ a: 1 }, false],
    ])('isNull(%p) → %p', (input, expected) => {
      expect(isNull(input)).toBe(expected);
    });

    it('treats nested empty arrays/objects as non-null', () => {
      expect(isNull([[]])).toBe(false);
      expect(isNull({ nested: {} })).toBe(false);
    });
  });
});
