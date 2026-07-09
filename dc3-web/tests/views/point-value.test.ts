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
  listPointByIds: vi.fn(() => Promise.resolve({data: {}})),
  listPointUnit: vi.fn(() => Promise.resolve({data: {}})),
  getPointValueLatest: vi.fn(() => Promise.resolve({data: {}})),
  listPointValue: vi.fn(() =>
    Promise.resolve({data: {records: [{id: 'pv-1', deviceId: 'd-1', pointId: 'pt-1', value: '42'}], total: 1}})
  ),
  writePointValue: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/point', () => pointMocks);
vi.mock('@/api/device', () => ({listDeviceByIds: vi.fn(() => Promise.resolve({data: {}}))}));
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));
vi.mock('@/views/point/value/tool/PointValueTool.vue', () => ({default: {template: '<div />'}}));
vi.mock('@/views/point/value/card/PointValueCard.vue', () => ({default: {template: '<div />'}}));
vi.mock('@/views/point/value/edit/PointValueEditForm.vue', () => ({default: {template: '<div />'}}));
vi.mock('@/views/point/value/detail/PointValueDetail.vue', () => ({default: {template: '<div />'}}));

describe('PointValue list view', () => {
  it('lists point values on mount', async () => {
    const PointValue = (await import('@/views/point/value/PointValue.vue')).default;
    await mountListPage({component: PointValue});
    await flushPromises();
    expect(pointMocks.listPointValue).toHaveBeenCalledTimes(1);
  });
});
