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
