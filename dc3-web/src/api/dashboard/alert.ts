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

import { httpGet, httpPost } from "@/api/common";
import { API_DATA_BASE } from "@/config/constant/api";
import type { PageResult } from "@/config/types";
import type {
  AgingBacklog,
  AlertActivityRow,
  AlertEventRow,
  AlertPageQuery,
  AlertSource,
  AlertStormRow,
  AlertTopSourceRow,
  AlertTrendRow,
  AlertTypeRow,
  ChangeImpact,
  CorrelationPair,
  FlappingSource,
  MttaTrend,
  PeerDeviation,
} from "@/config/types/dashboard";

/**
 * Fetch one page of alerts.
 * @param body - request body payload
 * @returns the page result response
 */
export const alertPage = (body: AlertPageQuery = {}) =>
  httpPost<PageResult<AlertEventRow>>(
    `${API_DATA_BASE}/dashboard/alert/page`,
    body,
  );

/**
 * Confirm a single alert.
 * @param source - alarm source filter
 * @param id - record id
 * @returns the boolean response
 */
export const alertConfirm = (source: AlertSource, id: string) =>
  httpPost<boolean>(`${API_DATA_BASE}/dashboard/alert/confirm`, undefined, {
    params: { source, id },
  });

/**
 * Unconfirm a single alert.
 * @param source - alarm source filter
 * @param id - record id
 * @returns the boolean response
 */
export const alertUnconfirm = (source: AlertSource, id: string) =>
  httpPost<boolean>(`${API_DATA_BASE}/dashboard/alert/unconfirm`, undefined, {
    params: { source, id },
  });

/**
 * Bulk-confirm alerts.
 * @param items - items to process
 * @param confirm - confirmation flag for destructive actions
 * @returns the number response
 */
export const alertBulkConfirm = (
  items: Array<{ source: AlertSource; id: string }>,
  confirm: boolean,
) =>
  httpPost<number>(`${API_DATA_BASE}/dashboard/alert/bulk_confirm`, {
    items,
    confirm,
  });

/**
 * Fetch the alert trend.
 * @param days - days entries
 * @returns the alert trend row response
 */
export const alertTrend = (days = 30) =>
  httpGet<AlertTrendRow[]>(`${API_DATA_BASE}/dashboard/alert/trend`, {
    params: { days },
  });

/**
 * Fetch the alert top sources.
 * @param days - days entries
 * @param limit - maximum number of entries to return or generate
 * @returns the alert top source row response
 */
export const alertTopSources = (days = 30, limit = 10) =>
  httpGet<AlertTopSourceRow[]>(
    `${API_DATA_BASE}/dashboard/alert/top_sources`,
    { params: { days, limit } },
  );

/**
 * Fetch the alert activity.
 * @param days - days entries
 * @returns the alert activity row response
 */
export const alertActivity = (days = 7) =>
  httpGet<AlertActivityRow[]>(`${API_DATA_BASE}/dashboard/alert/activity`, {
    params: { days },
  });

/**
 * Fetch the alert type distribution.
 * @param days - days entries
 * @returns the alert type row response
 */
export const alertTypeDistribution = (days = 30) =>
  httpGet<AlertTypeRow[]>(
    `${API_DATA_BASE}/dashboard/alert/type_distribution`,
    { params: { days } },
  );

/**
 * Fetch the alert storm sources.
 * @param hours - hours entries
 * @param minCount - min count
 * @param limit - maximum number of entries to return or generate
 * @returns the alert storm row response
 */
export const alertStormSources = (hours = 1, minCount = 10, limit = 10) =>
  httpGet<AlertStormRow[]>(
    `${API_DATA_BASE}/dashboard/alert/storm_sources`,
    {
      params: {
        hours,
        min_count: minCount,
        limit,
      },
    },
  );

/**
 * Fetch the alert flapping.
 * @param hours - hours entries
 * @param minCount - min count
 * @param limit - maximum number of entries to return or generate
 * @returns the flapping source response
 */
export const alertFlapping = (hours = 6, minCount = 5, limit = 20) =>
  httpGet<FlappingSource[]>(`${API_DATA_BASE}/dashboard/alert/flapping`, {
    params: {
      hours,
      min_count: minCount,
      limit,
    },
  });

/**
 * Fetch the alert correlation.
 * @param hours - hours entries
 * @param windowSec - window length in seconds
 * @param limit - maximum number of entries to return or generate
 * @returns the correlation pair response
 */
export const alertCorrelation = (hours = 24, windowSec = 30, limit = 15) =>
  httpGet<CorrelationPair[]>(
    `${API_DATA_BASE}/dashboard/alert/correlation`,
    {
      params: {
        hours,
        window_sec: windowSec,
        limit,
      },
    },
  );

/**
 * Fetch the alert peer deviation.
 * @param days - days entries
 * @returns the peer deviation response
 */
export const alertPeerDeviation = (days = 7) =>
  httpGet<PeerDeviation[]>(
    `${API_DATA_BASE}/dashboard/alert/peer_deviation`,
    { params: { days } },
  );

/**
 * Fetch the alert aging backlog.
 * @returns the aging backlog response
 */
export const alertAging = () =>
  httpGet<AgingBacklog>(`${API_DATA_BASE}/dashboard/alert/aging`);

/**
 * Fetch the alert MTTA trend.
 * @param days - days entries
 * @returns the mtta trend response
 */
export const alertMtta = (days = 30) =>
  httpGet<MttaTrend[]>(`${API_DATA_BASE}/dashboard/alert/mtta`, {
    params: { days },
  });

/**
 * Fetch the alert change impact.
 * @param days - days entries
 * @param limit - maximum number of entries to return or generate
 * @returns the change impact response
 */
export const alertChangeImpact = (days = 30, limit = 30) =>
  httpGet<ChangeImpact[]>(`${API_DATA_BASE}/dashboard/alert/change_impact`, {
    params: { days, limit },
  });
