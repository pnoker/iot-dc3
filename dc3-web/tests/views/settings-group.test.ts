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

const groupMocks = vi.hoisted(() => ({
  addGroup: vi.fn(() => Promise.resolve({data: true})),
  deleteGroup: vi.fn(() => Promise.resolve({data: true})),
  listGroup: vi.fn(() => Promise.resolve({data: {records: [{id: 'g-1', groupName: 'Default'}], total: 1}})),
  updateGroup: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/group', () => groupMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('SettingsGroup view', () => {
  it('lists groups on mount', async () => {
    const Group = (await import('@/views/settings/group/Group.vue')).default;
    await mountListPage({
      component: Group,
      stubs: {groupTool: {template: '<div />'}, groupAddForm: {template: '<div />'}},
    });
    await flushPromises();
    expect(groupMocks.listGroup.mock.calls.length).toBeGreaterThanOrEqual(1);
  });
});
