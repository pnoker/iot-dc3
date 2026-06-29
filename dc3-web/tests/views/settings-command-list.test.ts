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

const commandMocks = vi.hoisted(() => ({
  addCommand: vi.fn(() => Promise.resolve({data: true})),
  addCommandParam: vi.fn(() => Promise.resolve({data: true})),
  deleteCommand: vi.fn(() => Promise.resolve({data: true})),
  deleteCommandParam: vi.fn(() => Promise.resolve({data: true})),
  listCommand: vi.fn(() => Promise.resolve({data: {records: [], total: 0}})),
  updateCommand: vi.fn(() => Promise.resolve({data: true})),
  updateCommandParam: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/command', () => commandMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('CommandList view', () => {
  it('lists command definitions on mount', async () => {
    const CommandList = (await import('@/views/settings/command/CommandList.vue')).default;
    await mountListPage({
      component: CommandList,
      stubs: {
        CommandCard: {template: '<div />'},
        CommandTool: {template: '<div />'},
        CommandEditForm: {template: '<div />'},
      },
    });
    await flushPromises();
    expect(commandMocks.listCommand).toHaveBeenCalledTimes(1);
  });
});
