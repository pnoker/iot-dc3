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
