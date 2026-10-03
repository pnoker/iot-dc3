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

import {AUTH_HEADERS} from '@/config/constant/common';
import {getStorage, removeStorage} from '@/utils/storageUtil';
import {isNull} from '@/utils/validationUtil';

/**
 * Clear every frontend-held auth session artifact: the tenant/login header values
 * (local storage) and the authenticated flag (session storage). The auth token cookie
 * is httpOnly and cleared server-side; the caller owns the follow-up navigation and
 * user-notification policy.
 */
export const resetAuthSession = () => {
  removeStorage(AUTH_HEADERS.TENANT);
  removeStorage(AUTH_HEADERS.LOGIN);
  removeStorage(AUTH_HEADERS.AUTHENTICATED, true);
};

/**
 * Build the tenant/login auth header pair from storage for fetch callers that bypass
 * the axios interceptors. A header is omitted when its storage value is absent or empty.
 * The token cookie is never injected here.
 * @returns header names mapped to their string values
 */
export const buildAuthHeaders = (): Record<string, string> => {
  const headers: Record<string, string> = {};

  const tenant = getStorage(AUTH_HEADERS.TENANT);
  if (!isNull(tenant)) {
    headers[AUTH_HEADERS.TENANT] = String(tenant);
  }

  const login = getStorage(AUTH_HEADERS.LOGIN);
  if (!isNull(login)) {
    headers[AUTH_HEADERS.LOGIN] = String(login);
  }

  return headers;
};
