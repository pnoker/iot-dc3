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

const eventMocks = vi.hoisted(() => ({
  addEvent: vi.fn(() => Promise.resolve({data: true})),
  addEventParam: vi.fn(() => Promise.resolve({data: true})),
  deleteEvent: vi.fn(() => Promise.resolve({data: true})),
  deleteEventParam: vi.fn(() => Promise.resolve({data: true})),
  listEvent: vi.fn(() => Promise.resolve({data: {records: [], total: 0}})),
  updateEvent: vi.fn(() => Promise.resolve({data: true})),
  updateEventParam: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/event', () => eventMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('EventList view', () => {
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
});
