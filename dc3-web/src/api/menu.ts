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
import {API_AUTH_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {MenuForm, MenuRecord} from '@/config/types/auth';

export const addMenu = (menu: MenuForm) => httpPost(`${API_AUTH_BASE}/menu/add`, menu);

export const deleteMenu = (id: string) => httpPost(`${API_AUTH_BASE}/menu/delete`, undefined, {params: {id}});

export const updateMenu = (menu: MenuForm) => httpPost(`${API_AUTH_BASE}/menu/update`, menu);

export const getMenuById = (id: string) => httpGet(`${API_AUTH_BASE}/menu/get_by_id`, {params: {id}});

export const listMenu = <T = R<PageResult<MenuRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_AUTH_BASE}/menu/list`, query);

export const listMenuTree = (query: PageQuery = {}) => httpPost(`${API_AUTH_BASE}/menu/list_tree`, query);
