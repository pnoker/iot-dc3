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
import {createCrudApi} from '@/api/factory';
import {API_AUTH_BASE} from '@/config/constant/api';
import type {UserForm, UserRecord} from '@/config/types/auth';

const crud = createCrudApi<UserForm, UserRecord>({base: API_AUTH_BASE, entity: 'user_profile'});

export const addUser = crud.add;

export const deleteUser = crud.delete;

export const updateUser = crud.update;

export const getUserById = crud.getById;

export const getUserByName = (name: string) =>
  httpGet<R<UserRecord>>(`${API_AUTH_BASE}/user_profile/get_by_name`, {params: {name}});

export const listUser = crud.list;
