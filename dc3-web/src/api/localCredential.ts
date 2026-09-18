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
import {createCrudApi} from '@/api/factory';
import {API_LOCAL_CREDENTIAL_BASE} from '@/config/constant/api';
import type {LocalCredentialForm, LocalCredentialRecord} from '@/config/types/auth';

const crud = createCrudApi<LocalCredentialForm, LocalCredentialRecord>({base: API_LOCAL_CREDENTIAL_BASE});

export const addLocalCredential = crud.add;

export const deleteLocalCredential = crud.delete;

export const updateLocalCredential = crud.update;

export const resetLocalCredentialPassword = (id: string, password: string) =>
  httpPost<LocalCredentialRecord>(`${API_LOCAL_CREDENTIAL_BASE}/reset_password`, undefined, {params: {id, password}});

export const checkLoginNameAvailable = (name: string) =>
  httpGet<boolean>(`${API_LOCAL_CREDENTIAL_BASE}/check`, {params: {name}});

export const listLocalCredential = crud.list;
