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
import i18n from '@/config/i18n';
import router from '@/config/router';
import {buildAuthHeaders, resetAuthSession} from '@/utils/authSession';
import {failMessage, warnMessage} from '@/utils/notificationUtil';
import {notifyAggregatedFailure} from '@/utils/problemNotifier';

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
  // All toasts flow through the failure aggregator: a burst of failing
  // requests (dev stack without backend, gateway restart, polling loop)
  // shows at most a handful of notifications with repeat counts, not one
  // toast per request.
  if (status === AXIOS_CONFIG.UNAUTHORIZED_STATUS) {
    notifyAggregatedFailure(
        'auth-401',
        (message, title) => warnMessage(message, title),
        i18n.global.t('common.axios.unauthorizedTitle'),
        i18n.global.t('common.axios.unauthorized'));
    resetAuthSession();
    router.push({name: 'login'}).catch(() => {});
  } else if (status >= 500) {
    notifyAggregatedFailure(
        `http-${status}`,
        failMessage,
        i18n.global.t('common.axios.serverError'),
        i18n.global.t('common.axios.serverErrorMessage', {status}));
  } else if (status > 0) {
    // Same argument shape as before the aggregator: the request-error label
    // reads as the toast message, the problem code as the title.
    notifyAggregatedFailure(
        `code-${problem.code ?? status}`,
        failMessage,
        String(problem.code ?? problem.title ?? ''),
        i18n.global.t('common.axios.requestError'),
        problem);
  } else {
    notifyAggregatedFailure(
        'network',
        failMessage,
        i18n.global.t('common.axios.networkError'),
        i18n.global.t('common.axios.networkErrorMessage'));
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

    for (const [name, value] of Object.entries(buildAuthHeaders())) {
      headers[name] = value;
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
