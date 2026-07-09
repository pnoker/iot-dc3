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

import type {AxiosInstance} from 'axios';
import axios, {type AxiosError, type AxiosResponse, type InternalAxiosRequestConfig} from 'axios';
import {ElNotification} from 'element-plus';

import {AXIOS_CONFIG, AXIOS_ERROR_MESSAGES, PASSWORD_CHANGE_CODES} from '@/config/constant/axios';
import {AUTH_HEADERS} from '@/config/constant/common';
import {failMessage, warnMessage} from '@/utils/notificationUtil';
import {getStorage, removeStorage} from '@/utils/storageUtil';
import {isNull} from '@/utils/validationUtil';
import router from '@/config/router';
import JSONBigInt from 'json-bigint';

/**
 * JSONBigInt parser instance with storeAsString option
 */
const JSONBigIntStr = JSONBigInt({storeAsString: true});

/**
 * Transform response data using JSONBigInt to handle large integers
 *
 * @param data Raw response data
 * @returns Parsed data
 */
function transformResponse(data: any): any {
  if (typeof data !== 'string' || data === '') {
    return data;
  }
  try {
    return JSONBigIntStr.parse(data);
  } catch {
    return data;
  }
}

/**
 * Custom Axios instance with default configuration
 * Handles large integers via JSONBigInt and includes authentication headers
 */
const request: AxiosInstance = axios.create({
  timeout: AXIOS_CONFIG.TIMEOUT,
  withCredentials: true,
  headers: {Accept: AXIOS_CONFIG.HEADERS.ACCEPT, 'Content-Type': AXIOS_CONFIG.HEADERS.CONTENT_TYPE},
  validateStatus: (status) => status >= AXIOS_CONFIG.MIN_STATUS && status <= AXIOS_CONFIG.MAX_STATUS,
  transformResponse: [transformResponse],
});

/**
 * Request interceptor to add authentication headers
 */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const headers = config.headers;
    if (!headers) {
      return config;
    }

    const tenant = getStorage(AUTH_HEADERS.TENANT);
    if (!isNull(tenant)) {
      headers[AUTH_HEADERS.TENANT] = tenant;
    }

    const login = getStorage(AUTH_HEADERS.LOGIN);
    if (!isNull(login)) {
      headers[AUTH_HEADERS.LOGIN] = login;
    }

    const token = getStorage(AUTH_HEADERS.TOKEN);
    if (!isNull(token)) {
      headers[AUTH_HEADERS.TOKEN] = JSON.stringify(token);
    }

    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

/**
 * Response interceptor to handle responses and errors
 */
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const ok = response.data?.ok || false;
    const status = response.status || AXIOS_CONFIG.UNAUTHORIZED_STATUS;
    const responseType = response.config.responseType;

    // Handle blob response type (e.g., file downloads)
    if (responseType === 'blob') {
      return response;
    }

    // Return data if request was successful
    if (ok) {
      return response.data;
    }

    // Password change / expiry: a business outcome, not an error. Pass the payload
    // through silently so the login flow can open the password change dialog.
    if (PASSWORD_CHANGE_CODES.includes(response.data?.code)) {
      return Promise.reject(response.data);
    }

    // Handle unauthorized access
    if (status === AXIOS_CONFIG.UNAUTHORIZED_STATUS) {
      warnMessage(AXIOS_ERROR_MESSAGES.UNAUTHORIZED, AXIOS_ERROR_MESSAGES.UNAUTHORIZED_TITLE);
      // Remove auth keys only — never nuke entire localStorage
      removeStorage(AUTH_HEADERS.TENANT);
      removeStorage(AUTH_HEADERS.LOGIN);
      removeStorage(AUTH_HEADERS.TOKEN);
      router.push({name: 'login'}).catch(() => {
      });
    } else if (status >= 500) {
      ElNotification({
        title: 'Server Error',
        message: `The server encountered an error (${status}). Please try again later.`,
        type: 'error',
      });
    } else {
      failMessage(AXIOS_ERROR_MESSAGES.REQUEST_ERROR, response.data?.code, response.data);
    }
    // Reject with the server payload so callers can inspect code/message if needed.
    // Existing no-op `.catch(() => {})` sites remain valid because they ignore the argument.
    return Promise.reject(response.data ?? {status, message: 'Request failed'});
  },
  (error: AxiosError) => {
    if (!error.response) {
      // Network error — no response received
      ElNotification({
        title: 'Network Error',
        message: 'Unable to reach the server. Please check your connection.',
        type: 'error',
      });
    }
    return Promise.reject(error);
  }
);

export default request;
