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

import {httpDelete, httpGet, httpPost} from '@/api/common';
import {API_AUTH_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {UserForm, UserRecord} from '@/config/types/auth';

const prefix = `${API_AUTH_BASE}/user_profile`;
export const addUser = (form: UserForm) => httpPost<UserRecord>(`${prefix}/add`, form);
export const deleteUser = (id: string) => httpDelete<void>(`${prefix}/delete`, {params: {id}});
export const updateUser = (form: UserForm) => httpPost<UserRecord>(`${prefix}/update`, form);
export const getUserById = (id: string) => httpGet<UserRecord>(`${prefix}/get_by_id`, {params: {id}});

export const getUserByName = (name: string) =>
  httpGet<UserRecord>(`${API_AUTH_BASE}/user_profile/get_by_name`, {params: {name}});

export const listUser = (query: PageQuery) => httpPost<PageResult<UserRecord>>(`${prefix}/list`, query);
