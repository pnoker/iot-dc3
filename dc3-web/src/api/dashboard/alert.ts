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
import type {AlertPageQuery, AlertSource} from '@/config/types/dashboard';

export const alertPage = (body: AlertPageQuery = {}) => httpPost(`${API_DATA_BASE}/dashboard/alert/page`, body);

export const alertConfirm = (source: AlertSource, id: string | number) =>
  httpPost(`${API_DATA_BASE}/dashboard/alert/confirm`, undefined, {params: {source, id}});

export const alertUnconfirm = (source: AlertSource, id: string | number) =>
  httpPost(`${API_DATA_BASE}/dashboard/alert/unconfirm`, undefined, {params: {source, id}});

export const alertBulkConfirm = (items: Array<{ source: AlertSource; id: string | number }>, confirm: boolean) =>
  httpPost(`${API_DATA_BASE}/dashboard/alert/bulk_confirm`, {items, confirm});

export const alertTrend = (days = 30) => httpGet(`${API_DATA_BASE}/dashboard/alert/trend`, {params: {days}});

export const alertTopSources = (days = 30, limit = 10) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/top_sources`, {params: {days, limit}});

export const alertActivity = (days = 7) => httpGet(`${API_DATA_BASE}/dashboard/alert/activity`, {params: {days}});

export const alertTypeDistribution = (days = 30) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/type_distribution`, {params: {days}});

export const alertStormSources = (hours = 1, minCount = 10, limit = 10) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/storm_sources`, {params: {hours, min_count: minCount, limit}});

export const alertFlapping = (hours = 6, minCount = 5, limit = 20) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/flapping`, {params: {hours, min_count: minCount, limit}});

export const alertCorrelation = (hours = 24, windowSec = 30, limit = 15) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/correlation`, {params: {hours, window_sec: windowSec, limit}});

export const alertPeerDeviation = (days = 7) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/peer_deviation`, {params: {days}});

export const alertAging = () => httpGet(`${API_DATA_BASE}/dashboard/alert/aging`);

export const alertMtta = (days = 30) => httpGet(`${API_DATA_BASE}/dashboard/alert/mtta`, {params: {days}});

export const alertChangeImpact = (days = 30, limit = 30) =>
  httpGet(`${API_DATA_BASE}/dashboard/alert/change_impact`, {params: {days, limit}});
