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
