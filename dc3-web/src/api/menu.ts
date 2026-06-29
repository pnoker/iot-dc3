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
