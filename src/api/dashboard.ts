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

import { httpGet } from '@/api/common';

// Matches backend dc3-center-data /dashboard endpoints.
const BASE = 'api/v3/data/dashboard';

export const statsToday = () => httpGet(`${BASE}/stats/today`);

export const statsTimeseries = (params: { granularity?: 'hour' | 'day'; rangeHours?: number } = {}) => {
  const search = new URLSearchParams();
  if (params.granularity) search.set('granularity', params.granularity);
  if (params.rangeHours != null) search.set('rangeHours', String(params.rangeHours));
  const qs = search.toString();
  return httpGet(`${BASE}/stats/timeseries${qs ? `?${qs}` : ''}`);
};

export const statsTop = (
  params: {
    dimension?: 'device' | 'point' | 'driver';
    rangeHours?: number;
    limit?: number;
  } = {}
) => {
  const search = new URLSearchParams();
  if (params.dimension) search.set('dimension', params.dimension);
  if (params.rangeHours != null) search.set('rangeHours', String(params.rangeHours));
  if (params.limit != null) search.set('limit', String(params.limit));
  const qs = search.toString();
  return httpGet(`${BASE}/top${qs ? `?${qs}` : ''}`);
};

export const streamLatest = (size = 20) => httpGet(`${BASE}/stream?size=${size}`);

// Manager-side dashboard aggregates (driver/device distribution tabs).
const MANAGER_BASE = 'api/v3/manager/dashboard';

export const driverStats = () => httpGet(`${MANAGER_BASE}/driver/stats`);

export const deviceStats = (topN = 10) => httpGet(`${MANAGER_BASE}/device/stats?topN=${topN}`);
