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

// Misc-utils tests — small pure helpers that don't warrant their own file.
// Keep these grouped by `src/utils/<file>` so it stays obvious which module
// each describe block exercises. Bigger or impure utilities (validation,
// async loader, clipboard, date) live in their own dedicated test files.

import {afterEach, describe, expect, it} from 'vitest';

import i18n from '@/config/i18n';
import {resolveMenuTitle} from '@/utils/menuUtil';
import {pointTypeKey, rwFlagKey} from '@/utils/pointFormatUtil';
import {cleanSearchParams, resetSearchForm} from '@/utils/searchParamUtil';
import {getAllStorage, getStorage, removeStorage, setStorage} from '@/utils/storageUtil';
import {formatMs, humanDuration, parseDateSafe} from '@/utils/timeUtil';

describe('utils (misc)', () => {
  describe('searchParamUtil', () => {
    it('removes empty filter values but keeps meaningful falsy values', () => {
      expect(
        cleanSearchParams({
          keyword: 'device',
          enableFlag: '',
          rangeKey: undefined,
          deleted: null,
          tags: [],
          active: false,
          count: 0,
          ids: ['1'],
        })
      ).toEqual({
        keyword: 'device',
        active: false,
        count: 0,
        ids: ['1'],
      });
    });

    it('resets a mutable form model to defaults', () => {
      const form = {name: 'old', enableFlag: 'ENABLE', page: 2};

      resetSearchForm(form, {enableFlag: ''});

      expect(form).toEqual({enableFlag: ''});
    });
  });

  describe('storageUtil', () => {
    it('round-trips primitive and object values with type preservation', () => {
      setStorage('tenant', 'default');
      setStorage('count', 12);
      setStorage('enabled', false);
      setStorage('token', {salt: 's', token: 't'});

      expect(getStorage('tenant')).toBe('default');
      expect(getStorage('count')).toBe(12);
      expect(getStorage('enabled')).toBe(false);
      expect(getStorage('token')).toEqual({salt: 's', token: 't'});
      expect(
        getAllStorage()
          .map((item) => item.name)
          .sort()
      ).toEqual(['count', 'enabled', 'tenant', 'token']);
    });

    it('removes values from the selected storage', () => {
      setStorage('temporary', 'value');
      removeStorage('temporary');

      expect(getStorage('temporary')).toBeUndefined();
    });
  });

  describe('pointFormatUtil', () => {
    it('maps point flags to stable i18n keys', () => {
      expect(pointTypeKey('FLOAT')).toBe('dataType.float');
      expect(pointTypeKey('missing')).toBe('status.unknown');
      expect(rwFlagKey('READ_WRITE')).toBe('status.readWrite');
      expect(rwFlagKey('RW')).toBe('status.readWrite');
      expect(rwFlagKey()).toBe('status.unknown');
    });
  });

  describe('timeUtil', () => {
    it('parses backend date strings and formats durations', () => {
      expect(parseDateSafe('2026-05-13 12:30:00')).toBeInstanceOf(Date);
      expect(parseDateSafe('not-a-date')).toBeNull();
      expect(formatMs(null)).toBe('—');
      expect(formatMs(250)).toBe('250ms');
      expect(formatMs(1500)).toBe('1.5s');
      expect(humanDuration(42)).toBe('42s');
      expect(humanDuration(3600)).toBe('1h');
    });
  });

  describe('menuUtil', () => {
    // Locale is a global singleton — reset it after each case so order
    // doesn't matter and tests stay independent.
    const originalLocale = i18n.global.locale.value;
    afterEach(() => {
      i18n.global.locale.value = originalLocale;
    });

    it('prefers current locale title and falls back safely', () => {
      i18n.global.locale.value = 'zh';

      expect(
        resolveMenuTitle({
          menuName: 'Fallback',
          menuExt: {content: {titles: {zh: '设备', en: 'Device'}}},
        })
      ).toBe('设备');

      i18n.global.locale.value = 'en';

      expect(
        resolveMenuTitle({
          menuName: 'Fallback',
          menuExt: {content: {title: 'nav.home'}},
        })
      ).toBe('Home');

      expect(resolveMenuTitle({menuName: 'Plain'})).toBe('Plain');
      expect(resolveMenuTitle(null)).toBe('');
    });
  });
});
