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
import {API_MANAGER_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {GroupForm, GroupRecord} from '@/config/types/manager';

export const addGroup = (group: GroupForm) => httpPost(`${API_MANAGER_BASE}/group/add`, group);

export const deleteGroup = (id: string) => httpPost(`${API_MANAGER_BASE}/group/delete`, undefined, {params: {id}});

export const updateGroup = (group: GroupForm) => httpPost(`${API_MANAGER_BASE}/group/update`, group);

export const getGroupById = (id: string) =>
  httpGet<R<GroupRecord>>(`${API_MANAGER_BASE}/group/get_by_id`, {params: {id}});

export const listGroup = <T = R<PageResult<GroupRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_MANAGER_BASE}/group/list`, query);
