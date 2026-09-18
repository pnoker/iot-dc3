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

import {describe, expect, it} from 'vitest';
import {createMemoryHistory, createRouter} from 'vue-router';

import commonRoutes from '@/config/router/common';
import operateRoutes from '@/config/router/operate';
import settingsRoute from '@/config/router/settings';
import viewsRoute from '@/config/router/views';

const router = createRouter({
  history: createMemoryHistory(),
  routes: [...commonRoutes, viewsRoute, settingsRoute, ...operateRoutes],
});

describe('route hierarchy', () => {
  it('keeps entity list and operation pages under one stable layout record', () => {
    const list = router.resolve('/device');
    const edit = router.resolve('/device/edit?id=1');
    const profile = router.resolve('/profile');
    const profileEdit = router.resolve('/profile/edit?id=1');

    expect(list.name).toBe('device');
    expect(edit.name).toBe('deviceEdit');
    expect(profile.name).toBe('profile');
    expect(profileEdit.name).toBe('profileEdit');
    expect(edit.matched[0]).toBe(list.matched[0]);
    expect(profileEdit.matched[0]).toBe(profile.matched[0]);
  });

  it('keeps settings detail pages under the same settings layout as lists', () => {
    const list = router.resolve('/settings/api');
    const detail = router.resolve('/settings/api/detail?id=1');

    expect(list.name).toBe('settingsApi');
    expect(detail.name).toBe('settingsApiDetail');
    expect(detail.matched[0]).toBe(list.matched[0]);
    expect(operateRoutes).toHaveLength(0);
  });
});
