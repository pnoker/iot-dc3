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
