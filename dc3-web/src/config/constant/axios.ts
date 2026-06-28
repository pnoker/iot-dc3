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
