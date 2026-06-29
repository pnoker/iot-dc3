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

import {listMcpConnection} from '@/api/mcp';
import {MCP_PRINCIPAL_TYPE_OPTIONS} from '@/config/constant/enums';
import type {EntityListConfig} from '@/config/types/entityList';

import {principalNameRelation} from '../relations';

interface McpConnectionHandlers {
  onAddConnection: () => void;
  onConnectionInfo: (row: Record<string, any>) => void;
  onManageTools: (row: Record<string, any>) => void;
  onRevoke: (row: Record<string, any>) => void;
}

const PRINCIPAL_TYPE_OPTIONS = MCP_PRINCIPAL_TYPE_OPTIONS.map((o) => ({label: o.label, value: o.value}));

const includes = (value: unknown, keyword: string) =>
  String(value ?? '')
    .toLowerCase()
    .includes(keyword.toLowerCase());

// MCP OAuth connections. Backend `connection/list` returns a flat array with no
// filters, so search is applied client-side before wrapping into a single-page
// result. principalId → name resolves through the shared family relations loader.
export const createMcpConnectionConfig = (
  t: ComposerTranslation,
  handlers: McpConnectionHandlers
): EntityListConfig => ({
  name: 'mcp-connection',
  title: t('nav.settingsMcpConnection'),
  editable: false,
  searchFields: [
    {
      prop: 'connectionName',
      label: t('settings.mcp.connectionName'),
      kind: 'input',
      placeholder: t('settings.mcp.connectionName'),
    },
    {prop: 'principalType', label: t('settings.mcp.principalType'), kind: 'select', options: PRINCIPAL_TYPE_OPTIONS},
  ],
  columns: [
    {prop: 'connectionName', label: t('settings.mcp.connectionName'), minWidth: 180},
    {prop: 'clientId', label: t('settings.mcp.clientId'), kind: 'code', minWidth: 220},
    {
      prop: 'principalId',
      label: t('settings.mcp.principalId'),
      minWidth: 150,
      formatter: (row, ctx) => ctx.relations.principalName?.[String(row.principalId)] || String(row.principalId ?? '-'),
    },
    {prop: 'principalType', label: t('settings.mcp.principalType'), minWidth: 150},
    {prop: 'grantType', label: t('settings.mcp.grantType'), minWidth: 180},
    {prop: 'enableFlag', label: t('common.enable'), kind: 'enable', width: 90},
    {prop: 'lastUsedTime', label: t('settings.mcp.lastUsedTime'), kind: 'time', width: 170},
    {prop: 'revokeTime', label: t('settings.mcp.revokeTime'), kind: 'time', width: 170},
  ],
  fields: [],
  defaultForm: () => ({}),
  relations: [principalNameRelation()],
  list: async (query) => {
    const p = query as Record<string, any>;
    const res: any = await listMcpConnection();
    let records: Record<string, any>[] = Array.isArray(res?.data) ? res.data : [];
    if (p.connectionName) records = records.filter((r) => includes(r.connectionName, p.connectionName));
    if (p.principalType) records = records.filter((r) => r.principalType === p.principalType);
    return {data: {records, total: records.length}} as R;
  },
  toolbarActions: [
    {
      key: 'add-connection',
      label: t('settings.mcp.addConnection'),
      icon: 'Plus',
      onClick: handlers.onAddConnection,
    },
  ],
  extraActions: [
    {
      key: 'connection-info',
      label: t('settings.mcp.connectionInfo'),
      type: 'primary',
      onClick: handlers.onConnectionInfo,
    },
    {key: 'manage-tools', label: t('settings.mcp.manageTools'), type: 'primary', onClick: handlers.onManageTools},
    {key: 'revoke', label: t('settings.mcp.revoke'), type: 'danger', onClick: handlers.onRevoke},
  ],
  operationWidth: 280,
  emptyText: t('settings.mcp.empty'),
});
