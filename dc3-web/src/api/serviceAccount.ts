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

import {httpPost} from '@/api/common';
import {createCrudApi} from '@/api/factory';
import {API_SERVICE_ACCOUNT_BASE} from '@/config/constant/api';
import type {ServiceAccountForm, ServiceAccountRecord} from '@/config/types/auth';

const crud = createCrudApi<ServiceAccountForm, ServiceAccountRecord>({base: API_SERVICE_ACCOUNT_BASE});

export const addServiceAccount = crud.add;

export const deleteServiceAccount = crud.delete;

export const updateServiceAccount = crud.update;

export const enableServiceAccount = (id: string) =>
  httpPost(`${API_SERVICE_ACCOUNT_BASE}/enable`, undefined, {params: {id}});

export const disableServiceAccount = (id: string) =>
  httpPost(`${API_SERVICE_ACCOUNT_BASE}/disable`, undefined, {params: {id}});

export const getServiceAccountById = crud.getById;

export const listServiceAccount = crud.list;
