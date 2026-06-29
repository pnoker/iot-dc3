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

import {httpPost} from '@/api/common';
import {API_TENANT_MEMBERSHIP_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {TenantMembershipForm, TenantMembershipRecord} from '@/config/types/auth';

export const addTenantMembership = (body: TenantMembershipForm) => httpPost(`${API_TENANT_MEMBERSHIP_BASE}/add`, body);

export const deleteTenantMembership = (id: string) =>
  httpPost(`${API_TENANT_MEMBERSHIP_BASE}/delete`, undefined, {params: {id}});

export const listTenantMembership = <T = R<PageResult<TenantMembershipRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_TENANT_MEMBERSHIP_BASE}/list`, query);
