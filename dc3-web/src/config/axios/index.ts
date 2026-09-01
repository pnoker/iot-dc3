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
  validateStatus: (status) => status >= AXIOS_CONFIG.MIN_STATUS && status < 300,
});

type ProblemPayload = {
  status?: number;
  code?: string;
  title?: string;
  detail?: string;
  [key: string]: unknown;
};

const normalizeProblem = (status: number, data: unknown, fallback?: string): ProblemPayload => {
  if (data && typeof data === 'object') {
    return data as ProblemPayload;
  }
  return {status, title: fallback ?? 'HTTP request failed', detail: fallback ?? 'HTTP request failed'};
};

const notifyProblem = (status: number, problem: ProblemPayload) => {
  if (typeof problem.code === 'string' && PASSWORD_CHANGE_CODES.includes(problem.code as (typeof PASSWORD_CHANGE_CODES)[number])) {
    return;
  }
  if (status === AXIOS_CONFIG.UNAUTHORIZED_STATUS) {
    warnMessage(i18n.global.t('common.axios.unauthorized'), i18n.global.t('common.axios.unauthorizedTitle'));
    removeStorage(AUTH_HEADERS.TENANT);
    removeStorage(AUTH_HEADERS.LOGIN);
    removeStorage(AUTH_HEADERS.AUTHENTICATED, true);
    router.push({name: 'login'}).catch(() => {});
  } else if (status >= 500) {
    failMessage(i18n.global.t('common.axios.serverErrorMessage', {status}), i18n.global.t('common.axios.serverError'));
  } else if (status > 0) {
    failMessage(i18n.global.t('common.axios.requestError'), problem.code ?? problem.title, problem);
  } else {
    failMessage(i18n.global.t('common.axios.networkErrorMessage'), i18n.global.t('common.axios.networkError'));
  }
};

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
    if (response.status < AXIOS_CONFIG.MIN_STATUS || response.status >= 300) {
      const problem = normalizeProblem(response.status, response.data);
      notifyProblem(response.status, problem);
      return Promise.reject(problem);
    }

    const responseType = response.config.responseType;

    // Handle blob response type (e.g., file downloads)
    if (responseType === 'blob') {
      return response;
    }

    return response.data;
  },
  (error: AxiosError) => {
    if (axios.isCancel(error)) {
      return Promise.reject(error);
    }
    const status = error.response?.status ?? 0;
    const problem = normalizeProblem(status, error.response?.data, error.message);
    notifyProblem(status, problem);
    return Promise.reject(problem);
  }
);

export default request;
