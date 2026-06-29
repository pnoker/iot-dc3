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

import type {ComposerTranslation} from 'vue-i18n';

import {addTenantMembership, deleteTenantMembership, listTenantMembership} from '@/api/tenantMembership';
import type {EntityListConfig} from '@/config/types/entityList';

import {principalIdField, principalNameRelation} from '../relations';

const PRINCIPAL_TYPE_OPTIONS = [
  {label: 'USER', value: 'USER'},
  {label: 'SERVICE_ACCOUNT', value: 'SERVICE_ACCOUNT'},
  {label: 'SYSTEM', value: 'SYSTEM'},
];
const MEMBERSHIP_STATUS_OPTIONS = [
  {label: 'ACTIVE', value: 'ACTIVE'},
  {label: 'SUSPENDED', value: 'SUSPENDED'},
  {label: 'INVITED', value: 'INVITED'},
];

export const createTenantMembershipConfig = (t: ComposerTranslation): EntityListConfig => ({
  name: 'tenant-membership',
  title: t('nav.settingsTenantMembership'),
  editable: true,
  searchFields: [
    {prop: 'principalId', label: t('settings.tenantMembership.principalId'), kind: 'input'},
    {
      prop: 'membershipStatus',
      label: t('settings.tenantMembership.membershipStatus'),
      kind: 'select',
      options: MEMBERSHIP_STATUS_OPTIONS,
    },
  ],
  columns: [
    {
      prop: 'principalId',
      label: t('settings.tenantMembership.principalId'),
      minWidth: 160,
      formatter: (row, ctx) => ctx.relations.principalName?.[String(row.principalId)] || String(row.principalId ?? '-'),
    },
    {prop: 'principalType', label: t('settings.tenantMembership.principalType'), minWidth: 150},
    {
      prop: 'membershipStatus',
      label: t('settings.tenantMembership.membershipStatus'),
      kind: 'tag',
      options: MEMBERSHIP_STATUS_OPTIONS,
      minWidth: 150,
    },
    {prop: 'joinedTime', label: t('settings.tenantMembership.joinedTime'), kind: 'time', width: 165},
    {prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 165},
  ],
  fields: [
    principalIdField(t('settings.tenantMembership.principalId')),
    {
      prop: 'principalType',
      label: t('settings.tenantMembership.principalType'),
      kind: 'select',
      options: PRINCIPAL_TYPE_OPTIONS,
      required: true,
    },
    {
      prop: 'membershipStatus',
      label: t('settings.tenantMembership.membershipStatus'),
      kind: 'select',
      options: MEMBERSHIP_STATUS_OPTIONS,
      required: true,
    },
  ],
  defaultForm: () => ({principalId: '', principalType: 'USER', membershipStatus: 'ACTIVE'}),
  relations: [principalNameRelation()],
  list: listTenantMembership,
  add: addTenantMembership,
  remove: deleteTenantMembership,
  confirmDeleteText: t('settings.tenantMembership.confirmDelete'),
  emptyText: t('settings.tenantMembership.empty'),
});
