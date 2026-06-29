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

import {httpGet} from '@/api/common';
import {API_DATA_BASE} from '@/config/constant/api';
import type {AlertStatsSummary} from '@/config/types/dashboard';

export const alertStats = () => httpGet<R<AlertStatsSummary>>(`${API_DATA_BASE}/dashboard/alert/stats`);

export const alertLatest = (size = 10) => httpGet(`${API_DATA_BASE}/dashboard/alert/latest`, {params: {size}});

export const systemHealth = () => httpGet(`${API_DATA_BASE}/dashboard/system/health`);

export const protocolHealth = () => httpGet(`${API_DATA_BASE}/dashboard/protocol/health`);

export const silentSources = (baselineDays = 7, silentMinutes = 15, limit = 50) =>
  httpGet(`${API_DATA_BASE}/dashboard/silent/sources`, {
    params: {baseline_days: baselineDays, silent_minutes: silentMinutes, limit},
  });

export const coverageGap = (limit = 100) => httpGet(`${API_DATA_BASE}/dashboard/coverage/gap`, {params: {limit}});
