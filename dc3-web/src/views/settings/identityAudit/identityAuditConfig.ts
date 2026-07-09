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

import {listIdentityAudit} from '@/api/identityAudit';
import type {EntityListConfig} from '@/config/types/entityList';

import {principalNameRelation} from '../relations';

const ACTION_OPTIONS = [
  {label: 'CREATE', value: 'CREATE'},
  {label: 'UPDATE', value: 'UPDATE'},
  {label: 'DELETE', value: 'DELETE'},
  {label: 'ENABLE', value: 'ENABLE'},
  {label: 'DISABLE', value: 'DISABLE'},
];
const STATUS_OPTIONS = [
  {label: 'SUCCESS', value: 'SUCCESS'},
  {label: 'FAILURE', value: 'FAILURE'},
];

// Read-only audit log: family list page with no add/edit/delete. The backend
// returns the latest N rows (no real paging), so `list` wraps the array into a
// single-page result the EntityListPage contract expects.
export const createIdentityAuditConfig = (t: ComposerTranslation): EntityListConfig => ({
  name: 'identity-audit',
  editable: false,
  searchFields: [
    {prop: 'principalId', label: t('settings.identityAudit.principalId'), kind: 'input'},
    {prop: 'action', label: t('settings.identityAudit.action'), kind: 'select', options: ACTION_OPTIONS},
    {prop: 'resourceType', label: t('settings.identityAudit.resourceType'), kind: 'input'},
    {prop: 'status', label: t('settings.identityAudit.status'), kind: 'select', options: STATUS_OPTIONS},
  ],
  columns: [
    {prop: 'createTime', label: t('settings.identityAudit.createTime'), kind: 'time', width: 165},
    {
      prop: 'principalId',
      label: t('settings.identityAudit.principalId'),
      minWidth: 140,
      formatter: (row, ctx) => ctx.relations.principalName?.[String(row.principalId)] || String(row.principalId ?? '-'),
    },
    {prop: 'principalType', label: t('settings.identityAudit.principalType'), minWidth: 130},
    {prop: 'action', label: t('settings.identityAudit.action'), minWidth: 110},
    {prop: 'resourceType', label: t('settings.identityAudit.resourceType'), minWidth: 150},
    {prop: 'resourceName', label: t('settings.identityAudit.resourceName'), minWidth: 150},
    {prop: 'status', label: t('settings.identityAudit.status'), kind: 'tag', options: STATUS_OPTIONS, minWidth: 100},
    {prop: 'errorCode', label: t('settings.identityAudit.errorCode'), minWidth: 120},
  ],
  fields: [],
  defaultForm: () => ({}),
  relations: [principalNameRelation()],
  list: async (query) => {
    const p = query as Record<string, any>;
    const res: any = await listIdentityAudit({
      principalId: p.principalId,
      action: p.action,
      resourceType: p.resourceType,
      status: p.status,
      limit: 200,
    });
    const records = Array.isArray(res?.data) ? res.data : [];
    return {data: {records, total: records.length}} as R;
  },
  emptyText: t('settings.identityAudit.empty'),
});
