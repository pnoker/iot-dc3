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
