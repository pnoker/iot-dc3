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

import {flushPromises} from '@vue/test-utils';
import {describe, expect, it, vi} from 'vitest';

import {mountListPage} from './_helpers'; // DeviceEdit is the wizard that the recent thing-model-matrix work landed.

// DeviceEdit is the wizard that the recent thing-model-matrix work landed.
// We lock in the mount flow + the changeProfile fan-out (point/command/event
// sibling fetches) — the regression spot the new guardrail tries to catch
// statically. This view test catches the same intent at runtime.

const deviceMocks = vi.hoisted(() => ({
  getDeviceById: vi.fn(() =>
    Promise.resolve({
      id: 'dev-1',
      deviceName: 'Pump',
      driverId: 'drv-1',
      profileId: 'pf-1',
      enableFlag: 'ENABLE',
    })
  ),
  updateDevice: vi.fn(() => Promise.resolve( {})),
}));

const driverMocks = vi.hoisted(() => ({
  getDriverById: vi.fn(() => Promise.resolve( {id: 'drv-1', driverName: 'Modbus'})),
}));

const profileMocks = vi.hoisted(() => ({
  getProfileById: vi.fn(() => Promise.resolve( {id: 'pf-1', profileName: 'Sensor'})),
}));

const dictionaryMocks = vi.hoisted(() => ({
  getDriverDictionary: vi.fn(() => Promise.resolve( {items: []})),
  getProfileDictionary: vi.fn(() => Promise.resolve( {items: []})),
}));

const attributeMocks = vi.hoisted(() => ({
  listCommandAttributeByDriverId: vi.fn(() => Promise.resolve( [])),
  listDriverAttributeByDriverId: vi.fn(() => Promise.resolve( [])),
  listEventAttributeByDriverId: vi.fn(() => Promise.resolve( [])),
  listPointAttributeByDriverId: vi.fn(() => Promise.resolve( [])),
}));

const infoMocks = vi.hoisted(() => ({
  addCommandInfo: vi.fn(() => Promise.resolve( {})),
  addDriverInfo: vi.fn(() => Promise.resolve( {})),
  addEventInfo: vi.fn(() => Promise.resolve( {})),
  addPointInfo: vi.fn(() => Promise.resolve( {})),
  listCommandInfoByDeviceId: vi.fn(() => Promise.resolve( [])),
  listDriverInfoByDeviceId: vi.fn(() => Promise.resolve( [])),
  listEventInfoByDeviceId: vi.fn(() => Promise.resolve( [])),
  listPointInfoByDeviceId: vi.fn(() => Promise.resolve( [])),
  updateCommandInfo: vi.fn(() => Promise.resolve( {})),
  updateDriverInfo: vi.fn(() => Promise.resolve( {})),
  updateEventInfo: vi.fn(() => Promise.resolve( {})),
  updatePointInfo: vi.fn(() => Promise.resolve( {})),
}));

const pointMocks = vi.hoisted(() => ({
  listPointByProfileId: vi.fn(() => Promise.resolve( [])),
}));

const commandMocks = vi.hoisted(() => ({
  listCommandByProfileId: vi.fn(() => Promise.resolve( [])),
}));

const eventMocks = vi.hoisted(() => ({
  listEventByProfileId: vi.fn(() => Promise.resolve( [])),
}));

vi.mock('@/api/device', () => deviceMocks);
vi.mock('@/api/driver', () => driverMocks);
vi.mock('@/api/profile', () => profileMocks);
vi.mock('@/api/dictionary', () => dictionaryMocks);
vi.mock('@/api/attribute', () => attributeMocks);
vi.mock('@/api/info', () => infoMocks);
vi.mock('@/api/point', () => pointMocks);
vi.mock('@/api/command', () => commandMocks);
vi.mock('@/api/event', () => eventMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));
vi.mock('@/config/router', () => ({default: {push: vi.fn(() => Promise.resolve())}}));

describe('DeviceEdit view', () => {
  it('loads the device, profile, and driver definitions on mount', async () => {
    const DeviceEdit = (await import('@/views/device/edit/DeviceEdit.vue')).default;
    await mountListPage({
      component: DeviceEdit,
      routePath: '/test',
      routeQuery: {id: 'dev-1', active: '0'},
      stubs: {
        EnableFlagSegmented: {template: '<div />'},
      },
    });
    await flushPromises();

    expect(deviceMocks.getDeviceById).toHaveBeenCalledWith('dev-1');
    expect(driverMocks.getDriverById).toHaveBeenCalledWith('drv-1');
    expect(profileMocks.getProfileById).toHaveBeenCalledWith('pf-1');

    // listPointByProfileId must fire once on initial load — the regression
    // spot is "did changeProfile() lose any of its three sibling refreshes?"
    expect(pointMocks.listPointByProfileId).toHaveBeenCalled();
    expect(commandMocks.listCommandByProfileId).toHaveBeenCalled();
    expect(eventMocks.listEventByProfileId).toHaveBeenCalled();
  });
});
