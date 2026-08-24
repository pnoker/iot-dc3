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

import {AXIOS_CONFIG, PASSWORD_CHANGE_CODES} from '@/config/constant/axios';
import {AUTH_HEADERS} from '@/config/constant/common';
import i18n from '@/config/i18n';
import {failMessage, warnMessage} from '@/utils/notificationUtil';
import {getStorage, removeStorage} from '@/utils/storageUtil';
import {isNull} from '@/utils/validationUtil';
import router from '@/config/router';

/**
 * Custom Axios instance with default configuration
 * Includes authentication headers
 */
const request: AxiosInstance = axios.create({
  timeout: AXIOS_CONFIG.TIMEOUT,
  withCredentials: true,
  headers: {Accept: AXIOS_CONFIG.HEADERS.ACCEPT, 'Content-Type': AXIOS_CONFIG.HEADERS.CONTENT_TYPE},
  validateStatus: (status) => status >= AXIOS_CONFIG.MIN_STATUS && status <= AXIOS_CONFIG.MAX_STATUS,
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

    // Token travels in an httpOnly cookie (withCredentials) — never inject it
    // into a header the frontend can read.

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
      warnMessage(i18n.global.t('common.axios.unauthorized'), i18n.global.t('common.axios.unauthorizedTitle'));
      // Remove auth keys only — never nuke entire localStorage. The token cookie
      // is cleared server-side on 401; here we just drop the frontend flag.
      removeStorage(AUTH_HEADERS.TENANT);
      removeStorage(AUTH_HEADERS.LOGIN);
      removeStorage(AUTH_HEADERS.AUTHENTICATED, true);
      router.push({name: 'login'}).catch(() => {
      });
    } else if (status >= 500) {
      failMessage(i18n.global.t('common.axios.serverErrorMessage', {status}), i18n.global.t('common.axios.serverError'));
    } else {
      failMessage(i18n.global.t('common.axios.requestError'), response.data?.code, response.data);
    }
    // Reject with the server payload so callers can inspect code/message if needed.
    // Existing no-op `.catch(() => {})` sites remain valid because they ignore the argument.
    return Promise.reject(response.data ?? {status, message: 'Request failed'});
  },
  (error: AxiosError) => {
    if (!error.response) {
      // Network error — no response received
      failMessage(i18n.global.t('common.axios.networkErrorMessage'), i18n.global.t('common.axios.networkError'));
    }
    return Promise.reject(error);
  }
);

export default request;
