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
import type {
  AlertEventRow,
  AlertStatsSummary,
  CoverageGap,
  ProtocolHealth,
  SilentSource,
  SystemHealth
} from '@/config/types/dashboard';

/**
 * Fetch the alert stats.
 * @returns the alert stats summary response
 */
export const alertStats = () => httpGet<AlertStatsSummary>(`${API_DATA_BASE}/dashboard/alert/stats`);

/**
 * Fetch the alert latest.
 * @param limit - maximum number of entries to return or generate
 * @returns the alert event row response
 */
export const alertLatest = (limit = 10) =>
  httpGet<AlertEventRow[]>(`${API_DATA_BASE}/dashboard/alert/latest`, {params: {limit}});

/**
 * Fetch the system health.
 * @returns the system health response
 */
export const systemHealth = () => httpGet<SystemHealth>(`${API_DATA_BASE}/dashboard/system/health`);

/**
 * Fetch the protocol health.
 * @returns the protocol health response
 */
export const protocolHealth = () => httpGet<ProtocolHealth[]>(`${API_DATA_BASE}/dashboard/protocol/health`);

/**
 * Fetch the silent sources.
 * @param baselineDays - baseline days entries
 * @param silentMinutes - silent minutes entries
 * @param limit - maximum number of entries to return or generate
 * @returns the silent source response
 */
export const silentSources = (baselineDays = 7, silentMinutes = 15, limit = 50) =>
  httpGet<SilentSource[]>(`${API_DATA_BASE}/dashboard/silent/sources`, {
    params: {baseline_days: baselineDays, silent_minutes: silentMinutes, limit},
  });

/**
 * Fetch the coverage gap.
 * @param limit - maximum number of entries to return or generate
 * @returns the coverage gap response
 */
export const coverageGap = (limit = 100) =>
  httpGet<CoverageGap>(`${API_DATA_BASE}/dashboard/coverage/gap`, {params: {limit}});
