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
import {API_TENANT_MEMBERSHIP_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {TenantMembershipForm, TenantMembershipRecord} from '@/config/types/auth';

export const addTenantMembership = (body: TenantMembershipForm) => httpPost(`${API_TENANT_MEMBERSHIP_BASE}/add`, body);

export const deleteTenantMembership = (id: string) =>
  httpPost(`${API_TENANT_MEMBERSHIP_BASE}/delete`, undefined, {params: {id}});

export const listTenantMembership = <T = R<PageResult<TenantMembershipRecord>>>(query: PageQuery) =>
  httpPost<T>(`${API_TENANT_MEMBERSHIP_BASE}/list`, query);
