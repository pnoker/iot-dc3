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

const pointMocks = vi.hoisted(() => ({
  addPoint: vi.fn(() => Promise.resolve({data: true})),
  deletePoint: vi.fn(() => Promise.resolve({data: true})),
  listPoint: vi.fn(() =>
    Promise.resolve({data: {records: [{id: 'pt-1', pointName: 'Temp', profileId: 'pf-1'}], total: 1}})
  ),
  updatePoint: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/point', () => pointMocks);
vi.mock('@/api/profile', () => ({
  listProfileByIds: vi.fn(() => Promise.resolve({data: {'pf-1': {profileName: 'Sensor'}}})),
}));
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('Point list view', () => {
  it('lists points on mount', async () => {
    const Point = (await import('@/views/point/Point.vue')).default;
    await mountListPage({
      component: Point,
      stubs: {
        PointTool: {template: '<div />'},
        PointCard: {template: '<div />'},
        PointAddForm: {template: '<div />'},
      },
    });
    await flushPromises();
    expect(pointMocks.listPoint).toHaveBeenCalledTimes(1);
  });
});
