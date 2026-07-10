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
