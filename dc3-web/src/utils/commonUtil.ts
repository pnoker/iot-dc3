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

import router from '@/config/router';
import {useAuthStore} from '@/store';
import {setCopyContent} from '@/utils/clipboardUtil';

/**
 * Copy content to clipboard with success message
 *
 * @param content Content to copy
 * @param message Success message to display
 */
export const copy = (content: string, message: string) => {
  setCopyContent(content, true, message);
};

/**
 * Logout and redirect to login page
 */
export const logout = async () => {
  const authStore = useAuthStore();
  await authStore.logout();
  await router.push({name: 'login'});
};
