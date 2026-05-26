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

import { flushPromises } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

import { mountListPage } from './_helpers';

const pointMocks = vi.hoisted(() => ({
  listPointByIds: vi.fn(() => Promise.resolve({ data: {} })),
  getPointUnit: vi.fn(() => Promise.resolve({ data: {} })),
  getPointValueLatest: vi.fn(() => Promise.resolve({ data: {} })),
  listPointValue: vi.fn(() =>
    Promise.resolve({ data: { records: [{ id: 'pv-1', deviceId: 'd-1', pointId: 'pt-1', value: '42' }], total: 1 } })
  ),
  writePointValue: vi.fn(() => Promise.resolve({ data: true })),
}));

vi.mock('@/api/point', () => pointMocks);
vi.mock('@/api/device', () => ({ listDeviceByIds: vi.fn(() => Promise.resolve({ data: {} })) }));
vi.mock('@/utils/notificationUtil', () => ({ failMessage: vi.fn(), successMessage: vi.fn() }));

describe('PointValue list view', () => {
  // PointValue mount triggers four sibling fetches (latest / unit / device /
  // point) plus an interval store registration — relies on the 30s global
  // testTimeout in vitest.config.ts instead of the default 5s.
  it('lists point values on mount', async () => {
    const PointValue = (await import('@/views/point/value/PointValue.vue')).default;
    await mountListPage({
      component: PointValue,
      stubs: {
        pointValueTool: { template: '<div />' },
        pointValueCard: { template: '<div />' },
        pointValueEditForm: { template: '<div />' },
        pointValueDetail: { template: '<div />' },
      },
    });
    await flushPromises();
    expect(pointMocks.listPointValue).toHaveBeenCalledTimes(1);
  });
});
