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

const eventMocks = vi.hoisted(() => ({
  addEvent: vi.fn(() => Promise.resolve({id: '101'})),
  addEventParam: vi.fn(() => Promise.resolve({id: '201'})),
  deleteEvent: vi.fn(() => Promise.resolve( true)),
  deleteEventParam: vi.fn(() => Promise.resolve( true)),
  listEvent: vi.fn(() => Promise.resolve( {items: [], total: 0})),
  updateEvent: vi.fn(() => Promise.resolve( true)),
  updateEventParam: vi.fn(() => Promise.resolve( true)),
}));

vi.mock('@/api/event', () => eventMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('EventList view', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lists event definitions on mount', async () => {
    const EventList = (await import('@/views/settings/event/definition/EventList.vue')).default;
    await mountListPage({
      component: EventList,
      stubs: {
        EventCard: {template: '<div />'},
        EventTool: {template: '<div />'},
        EventEditForm: {template: '<div />'},
      },
    });
    await flushPromises();
    expect(eventMocks.listEvent).toHaveBeenCalledTimes(1);
  });

  it('uses the created event record id when saving parameters', async () => {
    const EventList = (await import('@/views/settings/event/definition/EventList.vue')).default;
    const wrapper = await mountListPage({
      component: EventList,
      stubs: {
        EventCard: {template: '<div />'},
        EventTool: {template: '<div />'},
        EventEditForm: {
          emits: ['add-thing'],
          template: '<button class="emit-add" @click="$emit(\'add-thing\', {eventName: \'alarm\', profileId: \'10\'}, [{paramName: \'temperature\', paramCode: \'temperature\'}], () => undefined)" />',
        },
      },
    });
    await wrapper.get('.emit-add').trigger('click');
    await flushPromises();
    expect(eventMocks.addEventParam).toHaveBeenCalledWith(expect.objectContaining({eventId: '101'}));
  });
});
