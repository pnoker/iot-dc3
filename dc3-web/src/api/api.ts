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

import {createCrudApi} from '@/api/factory';
import {API_AUTH_BASE} from '@/config/constant/api';
import type {ApiForm, ApiRecord} from '@/config/types/auth';

const crud = createCrudApi<ApiForm, ApiRecord>({base: API_AUTH_BASE, entity: 'api'});

export const addApi = crud.add;

export const deleteApi = crud.delete;

export const updateApi = crud.update;

export const getApiById = crud.getById;

export const listApi = crud.list;
