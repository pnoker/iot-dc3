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
