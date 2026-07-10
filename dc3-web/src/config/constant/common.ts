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

/**
 * Common constant values used throughout the application
 */

/**
 * HTTP header names for authentication
 */
export const AUTH_HEADERS = {
  /** Header name for tenant identification */
  TENANT: 'X-Auth-Tenant',
  /** Header name for login information */
  LOGIN: 'X-Auth-Login',
  /** Header name for authentication token */
  TOKEN: 'X-Auth-Token',
} as const;

/**
 * Default export for backward compatibility
 * @deprecated Use AUTH_HEADERS instead
 */
export default {
  X_AUTH_TENANT: AUTH_HEADERS.TENANT,
  X_AUTH_LOGIN: AUTH_HEADERS.LOGIN,
  X_AUTH_TOKEN: AUTH_HEADERS.TOKEN,
};
