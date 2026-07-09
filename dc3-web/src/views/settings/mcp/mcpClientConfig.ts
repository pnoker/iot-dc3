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

import {listMcpClient} from '@/api/mcp';
import {MCP_CLIENT_TYPE_OPTIONS} from '@/config/constant/enums';
import type {EntityListConfig} from '@/config/types/entityList';

interface McpClientHandlers {
  onRegister: () => void;
}

const CLIENT_TYPE_OPTIONS = MCP_CLIENT_TYPE_OPTIONS.map((o) => ({label: o.label, value: o.value}));

const includes = (value: unknown, keyword: string) =>
  String(value ?? '')
    .toLowerCase()
    .includes(keyword.toLowerCase());

// OAuth clients. Backend `client/list` returns a flat array with no filters, so
// search is applied client-side here before wrapping into a single-page result.
export const createMcpClientConfig = (t: ComposerTranslation, handlers: McpClientHandlers): EntityListConfig => ({
  name: 'mcp-client',
  title: t('nav.settingsMcpClient'),
  editable: false,
  searchFields: [
    {
      prop: 'clientName',
      label: t('settings.mcp.clientName'),
      kind: 'input',
      placeholder: t('settings.mcp.clientName'),
    },
    {prop: 'clientType', label: t('settings.mcp.clientType'), kind: 'select', options: CLIENT_TYPE_OPTIONS},
  ],
  columns: [
    {prop: 'clientName', label: t('settings.mcp.clientName'), minWidth: 180},
    {prop: 'clientId', label: t('settings.mcp.clientId'), kind: 'code', minWidth: 240},
    {prop: 'clientType', label: t('settings.mcp.clientType'), minWidth: 130},
    {prop: 'authorizationGrantTypes', label: t('settings.mcp.grantTypes'), minWidth: 220},
    {prop: 'scopes', label: t('settings.mcp.scopes'), minWidth: 240},
    {prop: 'enableFlag', label: t('common.enable'), kind: 'enable', width: 90},
  ],
  fields: [],
  defaultForm: () => ({}),
  list: async (query) => {
    const p = query as Record<string, any>;
    const res: any = await listMcpClient();
    let records: Record<string, any>[] = Array.isArray(res?.data) ? res.data : [];
    if (p.clientName) records = records.filter((r) => includes(r.clientName, p.clientName));
    if (p.clientType) records = records.filter((r) => r.clientType === p.clientType);
    return {data: {records, total: records.length}} as R;
  },
  toolbarActions: [
    {
      key: 'register-client',
      label: t('settings.mcp.registerClient'),
      icon: 'Plus',
      onClick: handlers.onRegister,
    },
  ],
  emptyText: t('settings.mcp.empty'),
});
