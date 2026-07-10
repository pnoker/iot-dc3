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
import {API_PRINCIPAL_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {PrincipalRecord} from '@/config/types/auth';

export const getPrincipalById = (id: string) =>
  httpGet<R<PrincipalRecord>>(`${API_PRINCIPAL_BASE}/get_by_id`, {params: {id}});

export const listPrincipal = <T = R<PageResult<PrincipalRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_PRINCIPAL_BASE}/list`, query);

// Batch-resolve principals by id → used to render principalId references in other
// lists as display names (settings family relations loader). Mirrors listDeviceByIds.
export const listPrincipalByIds = (ids: string[]) =>
  httpPost<R<PrincipalRecord[]>>(`${API_PRINCIPAL_BASE}/list_by_ids`, ids);

export const enablePrincipal = (id: string) => httpPost(`${API_PRINCIPAL_BASE}/enable`, undefined, {params: {id}});

export const disablePrincipal = (id: string) => httpPost(`${API_PRINCIPAL_BASE}/disable`, undefined, {params: {id}});
