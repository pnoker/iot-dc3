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

const labelMocks = vi.hoisted(() => ({
  addLabel: vi.fn(() => Promise.resolve({data: true})),
  deleteLabel: vi.fn(() => Promise.resolve({data: true})),
  listLabel: vi.fn(() => Promise.resolve({data: {records: [{id: 'l-1', labelName: 'PROD'}], total: 1}})),
  updateLabel: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/label', () => labelMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('SettingsLabel view', () => {
  it('lists labels on mount', async () => {
    const Label = (await import('@/views/settings/label/Label.vue')).default;
    await mountListPage({
      component: Label,
      stubs: {labelTool: {template: '<div />'}, labelAddForm: {template: '<div />'}},
    });
    await flushPromises();
    expect(labelMocks.listLabel).toHaveBeenCalledTimes(1);
  });
});
