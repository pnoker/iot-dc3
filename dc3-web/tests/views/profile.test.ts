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

const profileMocks = vi.hoisted(() => ({
  addProfile: vi.fn(() => Promise.resolve({data: true})),
  deleteProfile: vi.fn(() => Promise.resolve({data: true})),
  listProfile: vi.fn(() => Promise.resolve({data: {records: [{id: 'p-1', profileName: 'Sensor'}], total: 1}})),
  updateProfile: vi.fn(() => Promise.resolve({data: true})),
}));

vi.mock('@/api/profile', () => profileMocks);
vi.mock('@/utils/notificationUtil', () => ({failMessage: vi.fn(), successMessage: vi.fn()}));

describe('Profile list view', () => {
  it('lists profiles on mount', async () => {
    const Profile = (await import('@/views/profile/Profile.vue')).default;
    await mountListPage({
      component: Profile,
      stubs: {
        ProfileTool: {template: '<div />'},
        ProfileCard: {template: '<div />'},
        ProfileAddForm: {template: '<div />'},
      },
    });
    await flushPromises();
    expect(profileMocks.listProfile).toHaveBeenCalledTimes(1);
  });
});
