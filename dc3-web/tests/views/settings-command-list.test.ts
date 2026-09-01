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
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {mountListPage} from './_helpers';

const commandMocks = vi.hoisted(() => ({
  addCommand: vi.fn(() => Promise.resolve({id: '101'})),
  addCommandParam: vi.fn(() => Promise.resolve({id: '201'})),
  deleteCommand: vi.fn(() => Promise.resolve( true)),
  deleteCommandParam: vi.fn(() => Promise.resolve( true)),
  listCommand: vi.fn(() => Promise.resolve( {items: [], total: 0})),
  updateCommand: vi.fn(() => Promise.resolve( true)),
  updateCommandParam: vi.fn(() => Promise.resolve( true)),
}));

vi.mock('@/api/command', () => commandMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('CommandList view', () => {
  beforeEach(() => vi.clearAllMocks());

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

  it('uses the created command record id when saving parameters', async () => {
    const CommandList = (await import('@/views/settings/command/CommandList.vue')).default;
    const wrapper = await mountListPage({
      component: CommandList,
      stubs: {
        CommandCard: {template: '<div />'},
        CommandTool: {template: '<div />'},
        CommandEditForm: {
          emits: ['add-thing'],
          template: '<button class="emit-add" @click="$emit(\'add-thing\', {commandName: \'read\', profileId: \'10\'}, [{paramName: \'speed\', paramCode: \'speed\'}], () => undefined)" />',
        },
      },
    });
    await wrapper.get('.emit-add').trigger('click');
    await flushPromises();
    expect(commandMocks.addCommandParam).toHaveBeenCalledWith(expect.objectContaining({commandId: '101'}));
  });
});
