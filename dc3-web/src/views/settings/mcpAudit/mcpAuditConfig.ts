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

import {listMcpAudit} from '@/api/mcp';
import type {EntityListConfig} from '@/config/types/entityList';

import {principalNameRelation} from '../relations';

const STATUS_OPTIONS = [
  {label: 'SUCCESS', value: 'SUCCESS'},
  {label: 'ERROR', value: 'ERROR'},
  {label: 'DENIED', value: 'DENIED'},
];
const RISK_OPTIONS = [
  {label: 'LOW', value: 'LOW'},
  {label: 'MEDIUM', value: 'MEDIUM'},
  {label: 'HIGH', value: 'HIGH'},
];

// Read-only audit log: family list page, no add/edit/delete. Backend returns the
// latest N rows (no real paging); `list` wraps the array into a single-page result.
export const createMcpAuditConfig = (t: ComposerTranslation): EntityListConfig => ({
  name: 'mcp-audit',
  editable: false,
  searchFields: [
    {prop: 'principalId', label: t('settings.mcpAudit.principalId'), kind: 'input'},
    {prop: 'toolId', label: t('settings.mcpAudit.toolId'), kind: 'input'},
    {prop: 'status', label: t('settings.mcpAudit.status'), kind: 'select', options: STATUS_OPTIONS},
    {prop: 'riskLevel', label: t('settings.mcpAudit.riskLevel'), kind: 'select', options: RISK_OPTIONS},
  ],
  columns: [
    {prop: 'createTime', label: t('settings.mcpAudit.createTime'), kind: 'time', width: 165},
    {
      prop: 'principalId',
      label: t('settings.mcpAudit.principalId'),
      minWidth: 140,
      formatter: (row, ctx) => ctx.relations.principalName?.[String(row.principalId)] || String(row.principalId ?? '-'),
    },
    {prop: 'principalType', label: t('settings.mcpAudit.principalType'), minWidth: 130},
    {prop: 'toolName', label: t('settings.mcpAudit.toolName'), minWidth: 180},
    {prop: 'status', label: t('settings.mcpAudit.status'), kind: 'tag', options: STATUS_OPTIONS, minWidth: 100},
    {prop: 'riskLevel', label: t('settings.mcpAudit.riskLevel'), kind: 'tag', options: RISK_OPTIONS, minWidth: 100},
    {prop: 'durationMs', label: t('settings.mcpAudit.durationMs'), minWidth: 110},
    {prop: 'clientId', label: t('settings.mcpAudit.clientId'), minWidth: 160},
    {prop: 'errorCode', label: t('settings.mcpAudit.errorCode'), minWidth: 120},
    {prop: 'traceId', label: t('settings.mcpAudit.traceId'), minWidth: 200},
  ],
  fields: [],
  defaultForm: () => ({}),
  relations: [principalNameRelation()],
  list: async (query) => {
    const p = query as Record<string, any>;
    const res: any = await listMcpAudit({
      principalId: p.principalId,
      toolId: p.toolId,
      status: p.status,
      riskLevel: p.riskLevel,
      limit: 200,
    });
    const records = Array.isArray(res?.data) ? res.data : [];
    return {data: {records, total: records.length}} as R;
  },
  emptyText: t('settings.mcpAudit.empty'),
});
