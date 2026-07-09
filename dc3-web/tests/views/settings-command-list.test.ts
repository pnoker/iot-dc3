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
