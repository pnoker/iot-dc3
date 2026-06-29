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

import {flushPromises} from '@vue/test-utils';
import {describe, expect, it, vi} from 'vitest';

import {mountListPage} from './_helpers';

const driverMocks = vi.hoisted(() => ({
  listDriverStatus: vi.fn(() => Promise.resolve({data: {}})),
  listDriver: vi.fn(() => Promise.resolve({data: {records: [{id: 'd-1', driverName: 'Modbus'}], total: 1}})),
}));

vi.mock('@/api/driver', () => driverMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('Driver list view', () => {
  it('lists drivers on mount', async () => {
    const Driver = (await import('@/views/driver/Driver.vue')).default;
    await mountListPage({
      component: Driver,
      stubs: {DriverTool: {template: '<div />'}, DriverCard: {template: '<div />'}},
    });
    await flushPromises();
    expect(driverMocks.listDriver).toHaveBeenCalledTimes(1);
  });
});
