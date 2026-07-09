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

// Service-flavoured utils — utilities that touch external systems
// (clipboard, navigator, router, ElMessage / ElNotification, store).
// Pure helpers without side effects live in `utils-misc.test.ts` and the
// `*-util.test.ts` files dedicated to a single source module.

import {beforeEach, describe, expect, it, vi} from 'vitest';
import type {Router} from 'vue-router';

const elementPlusSpies = vi.hoisted(() => ({
  messageError: vi.fn(),
  messageSuccess: vi.fn(),
  notification: vi.fn(),
}));

const commonUtilSpies = vi.hoisted(() => ({
  logout: vi.fn(),
  push: vi.fn(() => Promise.resolve()),
}));

vi.mock('element-plus', async (importOriginal) => ({
  ...(await importOriginal<typeof import('element-plus')>()),
  ElMessage: {
    error: elementPlusSpies.messageError,
    success: elementPlusSpies.messageSuccess,
  },
  ElNotification: elementPlusSpies.notification,
}));

vi.mock('@/config/router', () => ({
  default: {
    push: commonUtilSpies.push,
  },
}));

vi.mock('@/store', () => ({
  useAuthStore: () => ({
    logout: commonUtilSpies.logout,
  }),
}));

// Minimal Router stand-in for jumpUtil tests. Only `push` is exercised, but
// we keep the shape promotable to a real Router cast so call sites stay
// properly typed.
function makeRouterStub(): Pick<Router, 'push'> {
  return {
    push: vi.fn(() => Promise.resolve()) as Router['push'],
  };
}

describe('utils (services)', () => {
  beforeEach(() => {
    // `clearMocks: true` is set globally in vitest.config.ts, but spies on
    // hoisted shared objects need an explicit clear because they live outside
    // each test's vi.clearAllMocks() reset surface.
    elementPlusSpies.messageError.mockClear();
    elementPlusSpies.messageSuccess.mockClear();
    elementPlusSpies.notification.mockClear();
    commonUtilSpies.logout.mockClear();
    commonUtilSpies.push.mockClear();
  });

  describe('asyncLoaderUtil', () => {
    it('tracks async loader success, swallowed errors, and rethrown errors', async () => {
      const {useAsyncLoader} = await import('@/utils/asyncLoaderUtil');
      const {loading, run} = useAsyncLoader();

      await expect(run(async () => 'ok')).resolves.toBe('ok');
      expect(loading.value).toBe(false);

      await expect(run(async () => Promise.reject(new Error('swallowed')))).resolves.toBeUndefined();
      expect(loading.value).toBe(false);

      await expect(run(async () => Promise.reject(new Error('rethrown')), {rethrow: true})).rejects.toThrow('rethrown');
      expect(loading.value).toBe(false);
    });
  });

  describe('dateUtil', () => {
    it('formats legacy dates and calculates date differences', async () => {
      const {calcDate, dateFormat, timestamp, timestampColumn} = await import('@/utils/dateUtil');
      const date = new Date(2026, 4, 13, 8, 9, 10, 11);

      expect(dateFormat(date)).toBe('2026-05-13 08:09:10');
      expect(timestamp('2026-05-13T08:09:10')).toContain('2026-05-13 08:09:10');
      expect(timestampColumn({}, {}, '')).toBe('');
      expect(timestampColumn({}, {}, '2026-05-13T08:09:10')).toContain('2026-05-13 08:09:10');
      expect(calcDate(new Date('2026-05-13T00:00:00Z'), new Date('2026-05-14T01:02:03Z'))).toMatchObject({
        days: 1,
        hours: 1,
        minutes: 2,
        seconds: 90123,
      });
    });
  });

  describe('formRuleUtil', () => {
    it('exposes stable form validation rule contracts', async () => {
      const {AUTH_NAME_PATTERN, DECIMAL_PATTERN, NAME_PATTERN, authNameRules, nameRules, remarkRules} =
        await import('@/utils/formRuleUtil');
      const t = vi.fn((key: string, args?: Record<string, unknown>) => `${key}:${args?.name ?? ''}`);

      expect(NAME_PATTERN.test('设备-01')).toBe(true);
      expect(NAME_PATTERN.test('_bad')).toBe(false);
      expect(AUTH_NAME_PATTERN.test('role.admin')).toBe(true);
      expect(AUTH_NAME_PATTERN.test('角色')).toBe(false);
      expect(DECIMAL_PATTERN.test('-12.345')).toBe(true);
      expect(DECIMAL_PATTERN.test('12.3456')).toBe(false);
      expect(nameRules(t, 'Device')).toHaveLength(3);
      expect(authNameRules(t, 'Role')).toHaveLength(3);
      expect(remarkRules(t)[0]).toMatchObject({max: 300, trigger: 'blur'});
    });
  });

  describe('commonUtil', () => {
    it('copies content and logs out through the shared helpers', async () => {
      const {copy, logout} = await import('@/utils/commonUtil');
      const writeText = vi.fn(() => Promise.resolve());
      Object.defineProperty(navigator, 'clipboard', {
        configurable: true,
        value: {writeText},
      });

      copy('driver-1', 'Driver');
      await logout();

      await vi.waitFor(() => expect(writeText).toHaveBeenCalledWith('driver-1'));
      expect(commonUtilSpies.logout).toHaveBeenCalledTimes(1);
      expect(commonUtilSpies.push).toHaveBeenCalledWith({name: 'login'});
    });
  });

  describe('jumpUtil', () => {
    it('routes dashboard jumps to the expected named routes', async () => {
      const {jumpToEntity, jumpToSourceEvents} = await import('@/utils/jumpUtil');
      const router = makeRouterStub();

      jumpToEntity(router as Router, 'driver', 1);
      jumpToEntity(router as Router, 'device', 2);
      jumpToEntity(router as Router, 'profile', 3);
      jumpToEntity(router as Router, 'point', 4);
      jumpToEntity(router as Router, 'driver', '');
      jumpToSourceEvents(router as Router, 'device', 5);
      jumpToSourceEvents(router as Router, 'driver', 6);
      jumpToSourceEvents(router as Router, 'point', 7);

      expect(router.push).toHaveBeenCalledWith({name: 'driverDetail', query: {id: '1', active: 'detail'}});
      expect(router.push).toHaveBeenCalledWith({name: 'deviceDetail', query: {id: '2', active: 'detail'}});
      expect(router.push).toHaveBeenCalledWith({name: 'profileDetail', query: {id: '3', active: 'detail'}});
      expect(router.push).toHaveBeenCalledWith({name: 'pointValue', query: {pointId: '4'}});
      expect(router.push).toHaveBeenCalledWith({name: 'settingsDeviceAlarm', query: {sourceId: '5'}});
      expect(router.push).toHaveBeenCalledWith({name: 'settingsDriverAlarm', query: {sourceId: '6'}});
      expect(router.push).toHaveBeenCalledWith({name: 'settingsPointAlarm', query: {sourceId: '7'}});
      expect(router.push).toHaveBeenCalledTimes(7);
    });
  });

  describe('clipboardUtil', () => {
    it('copies with clipboard API and fallback DOM copy', async () => {
      const {setCopyContent} = await import('@/utils/clipboardUtil');
      const writeText = vi.fn(() => Promise.resolve());
      Object.defineProperty(navigator, 'clipboard', {
        configurable: true,
        value: {writeText},
      });

      await expect(setCopyContent('abc', true, 'ID')).resolves.toBe(true);
      expect(writeText).toHaveBeenCalledWith('abc');
      expect(elementPlusSpies.messageSuccess).toHaveBeenCalledWith({message: 'Copied ID to clipboard!'});

      writeText.mockRejectedValueOnce(new Error('denied'));
      Object.defineProperty(document, 'execCommand', {
        configurable: true,
        value: vi.fn(),
      });
      const execCommand = vi.spyOn(document, 'execCommand').mockReturnValue(true);

      await expect(setCopyContent('fallback', false, '')).resolves.toBe(true);
      expect(execCommand).toHaveBeenCalledWith('copy');
    });
  });

  describe('notificationUtil', () => {
    it('surfaces notification defaults and provided messages', async () => {
      const {failMessage, successMessage, warnMessage} = await import('@/utils/notificationUtil');

      successMessage();
      warnMessage('Careful', 'Heads up');
      failMessage('', 'Broken');

      expect(elementPlusSpies.notification).toHaveBeenCalledWith(
        expect.objectContaining({type: 'success', title: 'Success', message: 'Operation successful!'})
      );
      expect(elementPlusSpies.notification).toHaveBeenCalledWith(
        expect.objectContaining({type: 'warning', title: 'Heads up', message: 'Careful'})
      );
      expect(elementPlusSpies.notification).toHaveBeenCalledWith(
        expect.objectContaining({type: 'error', title: 'Broken', message: 'Operation failed!'})
      );
    });
  });
});
