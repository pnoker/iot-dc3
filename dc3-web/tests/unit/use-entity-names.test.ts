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

import {beforeEach, describe, expect, it, vi} from 'vitest';

const apiMocks = vi.hoisted(() => ({
  listDeviceByIds: vi.fn(),
  listDriverByIds: vi.fn(),
  listProfileByIds: vi.fn(),
  listPointByIds: vi.fn(),
}));

vi.mock('@/api/device', () => ({listDeviceByIds: apiMocks.listDeviceByIds}));
vi.mock('@/api/driver', () => ({listDriverByIds: apiMocks.listDriverByIds}));
vi.mock('@/api/profile', () => ({listProfileByIds: apiMocks.listProfileByIds}));
vi.mock('@/api/point', () => ({listPointByIds: apiMocks.listPointByIds}));

// Module under test imports the mocked APIs at module scope, so we must
// import it AFTER vi.mock — but the cache is module-level, so each test
// file resets through vi.resetModules() in beforeEach to keep tests
// independent.
async function loadHook() {
  const mod = await import('@/composables/useEntityNames');
  return mod.useEntityNames();
}

describe('useEntityNames', () => {
  beforeEach(async () => {
    vi.clearAllMocks();
    // Reset the module so the in-module reactive cache is empty at the
    // start of each test — otherwise the cache leaks between tests.
    vi.resetModules();

    apiMocks.listDeviceByIds.mockResolvedValue({
      data: {'d-1': {deviceName: 'Boiler'}, 'd-2': {deviceName: 'Compressor'}},
    });
    apiMocks.listDriverByIds.mockResolvedValue({
      data: {'drv-1': {driverName: 'Modbus'}},
    });
    apiMocks.listProfileByIds.mockResolvedValue({
      data: {'p-1': {profileName: 'TempSensor'}},
    });
    apiMocks.listPointByIds.mockResolvedValue({
      data: {'pt-1': {pointName: 'Inlet temperature'}},
    });
  });

  it('falls back to the id when the cache is cold and resolves names after fetching', async () => {
    const hook = await loadHook();

    // Cold cache → display name falls back to the id string.
    expect(hook.deviceName('d-1')).toBe('d-1');

    await hook.resolveDevices(['d-1', 'd-2']);

    expect(apiMocks.listDeviceByIds).toHaveBeenCalledTimes(1);
    expect(apiMocks.listDeviceByIds).toHaveBeenCalledWith(['d-1', 'd-2']);

    expect(hook.deviceName('d-1')).toBe('Boiler');
    expect(hook.deviceName('d-2')).toBe('Compressor');
  });

  it('coerces null/undefined ids to empty string instead of throwing', async () => {
    const hook = await loadHook();

    expect(hook.deviceName(null)).toBe('');
    expect(hook.deviceName(undefined)).toBe('');
    expect(hook.driverName(null)).toBe('');
    expect(hook.profileName(null)).toBe('');
    expect(hook.pointName(null)).toBe('');
  });

  it('coerces numeric ids to strings before lookup', async () => {
    apiMocks.listDeviceByIds.mockResolvedValueOnce({data: {'42': {deviceName: 'Mixer'}}});

    const hook = await loadHook();
    await hook.resolveDevices([42]);

    expect(apiMocks.listDeviceByIds).toHaveBeenCalledWith(['42']);
    expect(hook.deviceName(42)).toBe('Mixer');
  });

  it('does not refetch ids that are already cached', async () => {
    const hook = await loadHook();

    await hook.resolveDevices(['d-1']);
    await hook.resolveDevices(['d-1']);
    await hook.resolveDevices(['d-1']);

    // Only the first call hits the API; subsequent calls are served from cache.
    expect(apiMocks.listDeviceByIds).toHaveBeenCalledTimes(1);
  });

  it('caches the id itself when the backend returns no name (deleted entity)', async () => {
    apiMocks.listDriverByIds.mockResolvedValueOnce({data: {}});

    const hook = await loadHook();
    await hook.resolveDrivers(['drv-missing']);

    // Cache contains the id-as-name fallback, so subsequent calls see a
    // truthy hit and do NOT re-issue the request — we don't want to keep
    // re-fetching deleted entities every time a historical row references them.
    expect(hook.driverName('drv-missing')).toBe('drv-missing');

    await hook.resolveDrivers(['drv-missing']);
    expect(apiMocks.listDriverByIds).toHaveBeenCalledTimes(1);
  });

  it('swallows API errors silently — UI falls back to the id', async () => {
    apiMocks.listProfileByIds.mockRejectedValueOnce(new Error('500'));
    const hook = await loadHook();

    // Must not throw — global axios interceptor surfaces the error toast.
    await expect(hook.resolveProfiles(['p-err'])).resolves.toBeUndefined();
    expect(hook.profileName('p-err')).toBe('p-err');
  });

  it('dedupes inflight requests when the same ids are resolved concurrently', async () => {
    let resolveFetch!: (value: unknown) => void;
    apiMocks.listDeviceByIds.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveFetch = resolve;
      })
    );

    const hook = await loadHook();

    // Two concurrent resolveDevices calls for the same ids — only one HTTP
    // request should fire because the second sees the ids in inflight.
    const first = hook.resolveDevices(['d-1']);
    const second = hook.resolveDevices(['d-1']);

    resolveFetch({data: {'d-1': {deviceName: 'Boiler'}}});
    await Promise.all([first, second]);

    expect(apiMocks.listDeviceByIds).toHaveBeenCalledTimes(1);
    expect(hook.deviceName('d-1')).toBe('Boiler');
  });

  it('resolveBySource splits device/driver ids into a single batched fetch each', async () => {
    const hook = await loadHook();

    await hook.resolveBySource([
      {source: 'device', sourceId: 'd-1'},
      {source: 'driver', sourceId: 'drv-1'},
      {source: 'device', sourceId: 'd-2'},
    ]);

    expect(apiMocks.listDeviceByIds).toHaveBeenCalledTimes(1);
    expect(apiMocks.listDeviceByIds).toHaveBeenCalledWith(['d-1', 'd-2']);
    expect(apiMocks.listDriverByIds).toHaveBeenCalledTimes(1);
    expect(apiMocks.listDriverByIds).toHaveBeenCalledWith(['drv-1']);
  });

  it('nameBySource routes to deviceName / driverName by source', async () => {
    const hook = await loadHook();
    await hook.resolveBySource([
      {source: 'device', sourceId: 'd-1'},
      {source: 'driver', sourceId: 'drv-1'},
    ]);

    expect(hook.nameBySource('device', 'd-1')).toBe('Boiler');
    expect(hook.nameBySource('driver', 'drv-1')).toBe('Modbus');
  });

  it('handles empty input arrays without firing any request', async () => {
    const hook = await loadHook();

    await hook.resolveDevices([]);
    await hook.resolveDrivers([]);
    await hook.resolveProfiles([]);
    await hook.resolvePoints([]);

    expect(apiMocks.listDeviceByIds).not.toHaveBeenCalled();
    expect(apiMocks.listDriverByIds).not.toHaveBeenCalled();
    expect(apiMocks.listProfileByIds).not.toHaveBeenCalled();
    expect(apiMocks.listPointByIds).not.toHaveBeenCalled();
  });
});
