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
