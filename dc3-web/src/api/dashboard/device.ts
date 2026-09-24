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

import {httpGet, httpPost} from '@/api/common';
import {API_DATA_BASE} from '@/config/constant/api';
import type {PageResult} from '@/config/types';
import type {
  AlertEventRow,
  AlertPageQuery,
  DeviceActivityCell,
  DeviceAlertTrendPoint,
  DeviceCoverageGap,
  DeviceLatencyBucket,
  DeviceQuality,
  DeviceSilentSource,
  DeviceStatusDetail,
  DeviceStreamRow,
  DeviceTimeseriesPoint,
  DeviceTopPoint,
  Granularity,
  TimeRangeParams,
} from '@/config/types/dashboard';

/**
 * Fetch the device stats timeseries.
 * @param deviceId - device id to scope the request
 * @param params - query parameters for the request
 * @returns the device timeseries point response
 */
export const deviceTimeseries = (
  deviceId: string,
  params: TimeRangeParams & {granularity?: Granularity} = {},
) =>
  httpGet<DeviceTimeseriesPoint[]>(
    `${API_DATA_BASE}/dashboard/device/${deviceId}/stats/timeseries`,
    {params: timeRangeParams(params)},
  );

/**
 * Fetch the device stats latency.
 * @param deviceId - device id to scope the request
 * @param params - query parameters for the request
 * @returns the device latency bucket response
 */
export const deviceLatency = (deviceId: string, params: TimeRangeParams = {}) =>
  httpGet<DeviceLatencyBucket[]>(
    `${API_DATA_BASE}/dashboard/device/${deviceId}/stats/latency`,
    {params: timeRangeParams(params)},
  );

/**
 * Fetch the device stats activity.
 * @param deviceId - device id to scope the request
 * @param params - query parameters for the request
 * @returns the device activity cell response
 */
export const deviceActivity = (deviceId: string, params: TimeRangeParams = {}) =>
  httpGet<DeviceActivityCell[]>(
    `${API_DATA_BASE}/dashboard/device/${deviceId}/stats/activity`,
    {params: timeRangeParams(params)},
  );

/**
 * Fetch the device stats quality.
 * @param deviceId - device id to scope the request
 * @param params - query parameters for the request
 * @param params.rangeHours - lookback window in hours
 * @returns the device quality response
 */
export const deviceQuality = (deviceId: string, params: {rangeHours?: number} = {}) =>
  httpGet<DeviceQuality>(
    `${API_DATA_BASE}/dashboard/device/${deviceId}/stats/quality`,
    {params: timeRangeParams(params)},
  );

/**
 * Stream latest values for a device.
 * @param deviceId - device id to scope the request
 * @param limit - maximum number of entries to return or generate
 * @returns the device stream row response
 */
export const deviceStream = (deviceId: string, limit = 20) =>
  httpGet<DeviceStreamRow[]>(
    `${API_DATA_BASE}/dashboard/device/${deviceId}/stream`,
    {params: {limit}},
  );

/**
 * Fetch the device top points.
 * @param deviceId - device id to scope the request
 * @param params - query parameters for the request
 * @param params.rangeHours - lookback window in hours
 * @param params.limit - maximum number of entries to return or generate
 * @returns the device top point response
 */
export const deviceTopPoints = (
  deviceId: string,
  params: {rangeHours?: number; limit?: number} = {},
) =>
  httpGet<DeviceTopPoint[]>(
    `${API_DATA_BASE}/dashboard/device/${deviceId}/top/points`,
    {params: timeRangeParams(params)},
  );

/**
 * Fetch the device coverage gap.
 * @param deviceId - device id to scope the request
 * @returns the device coverage gap response
 */
export const deviceCoverageGap = (deviceId: string) =>
  httpGet<DeviceCoverageGap>(
    `${API_DATA_BASE}/dashboard/device/${deviceId}/coverage/gap`,
  );

/**
 * Fetch the device silent sources.
 * @param deviceId - device id to scope the request
 * @param params - query parameters for the request
 * @param params.baselineDays - baseline window in days for the silence rule
 * @param params.silentMinutes - silence threshold in minutes
 * @param params.limit - maximum number of entries to return or generate
 * @returns the device silent source response
 */
export const deviceSilentSources = (
  deviceId: string,
  params: {baselineDays?: number; silentMinutes?: number; limit?: number} = {},
) =>
  httpGet<DeviceSilentSource[]>(
    `${API_DATA_BASE}/dashboard/device/${deviceId}/silent/sources`,
    {
      params: {
        ...(params.baselineDays !== undefined ? {baseline_days: params.baselineDays} : {}),
        ...(params.silentMinutes !== undefined ? {silent_minutes: params.silentMinutes} : {}),
        ...(params.limit !== undefined ? {limit: params.limit} : {}),
      },
    },
  );

/**
 * Fetch the device alert trend.
 * @param deviceId - device id to scope the request
 * @param days - days entries
 * @returns the device alert trend point response
 */
export const deviceAlertTrend = (deviceId: string, days = 30) =>
  httpGet<DeviceAlertTrendPoint[]>(
    `${API_DATA_BASE}/dashboard/device/${deviceId}/alert/trend`,
    {params: {days}},
  );

/**
 * Fetch the device status detail.
 * @param deviceId - device id to scope the request
 * @returns the device status detail response
 */
export const deviceStatusDetail = (deviceId: string) =>
  httpGet<DeviceStatusDetail>(
    `${API_DATA_BASE}/device/status/${deviceId}`,
  );

/**
 * Fetch one page of device-scoped alerts.
 * @param body - request body payload
 * @returns the page result response
 */
export const deviceAlertPage = (body: AlertPageQuery = {}) =>
  httpPost<PageResult<AlertEventRow>, AlertPageQuery>(
    `${API_DATA_BASE}/dashboard/alert/page`,
    body,
  );

const timeRangeParams = <T extends TimeRangeParams>(params: T) => {
  const {rangeKey, rangeHours, ...rest} = params;
  return {
    ...rest,
    ...(rangeKey !== undefined ? {range_key: rangeKey} : {}),
    ...(rangeHours !== undefined ? {range_hours: rangeHours} : {}),
  };
};
