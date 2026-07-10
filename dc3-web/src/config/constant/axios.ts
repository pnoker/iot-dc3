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

/** Axios instance configuration */
export const AXIOS_CONFIG = {
  TIMEOUT: 15000,
  MIN_STATUS: 200,
  MAX_STATUS: 599,
  UNAUTHORIZED_STATUS: 401,
  HEADERS: {
    ACCEPT: 'application/json',
    CONTENT_TYPE: 'application/json',
  },
} as const;

/** Error messages for Axios interceptors */
export const AXIOS_ERROR_MESSAGES = {
  UNAUTHORIZED: 'You are not logged in or your login credentials have expired. Please log in again!',
  UNAUTHORIZED_TITLE: 'Login credentials expired',
  REQUEST_ERROR: 'API request error. Please contact the system administrator.',
} as const;

/**
 * Business response codes the interceptor must pass through silently so the calling
 * flow can react (e.g. drive the user to the password change dialog) instead of the
 * generic error toast. Mirrors backend ErrorCode (R4031 change required, R4032 expired).
 */
export const PASSWORD_CHANGE_CODES = ['R4031', 'R4032'] as const;
