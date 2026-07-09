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

import {listMcpTool} from '@/api/mcp';
import {MCP_RISK_LEVEL_OPTIONS} from '@/config/constant/enums';
import type {EntityListConfig} from '@/config/types/entityList';

interface McpToolHandlers {
  onRefresh: () => void;
  refreshing: () => boolean;
}

const RISK_OPTIONS = MCP_RISK_LEVEL_OPTIONS.map((o) => ({label: o.label, value: o.value}));

// Read-only tool catalog. Backend `tool/list` filters by keyword/riskLevel and
// returns a flat array (no real paging); `list` wraps it into a single-page result
// like the sibling MCP audit page.
export const createMcpToolConfig = (t: ComposerTranslation, handlers: McpToolHandlers): EntityListConfig => ({
  name: 'mcp-tool',
  title: t('nav.settingsMcpTool'),
  editable: false,
  searchFields: [
    {prop: 'keyword', label: t('settings.mcp.toolName'), kind: 'input', placeholder: t('settings.mcp.keyword')},
    {prop: 'riskLevel', label: t('settings.mcp.riskLevel'), kind: 'select', options: RISK_OPTIONS},
  ],
  columns: [
    {prop: 'toolName', label: t('settings.mcp.toolName'), minWidth: 220},
    {prop: 'toolTitle', label: t('settings.mcp.toolTitle'), minWidth: 180},
    {prop: 'serviceName', label: t('settings.mcp.serviceName'), minWidth: 150},
    {prop: 'riskLevel', label: t('settings.mcp.riskLevel'), kind: 'tag', options: RISK_OPTIONS, width: 110},
    {prop: 'permissionCode', label: t('settings.mcp.permissionCode'), kind: 'code', minWidth: 240},
    {prop: 'httpMethod', label: t('settings.mcp.httpMethod'), width: 95},
    {prop: 'apiPath', label: t('settings.mcp.apiPath'), minWidth: 220},
  ],
  fields: [],
  defaultForm: () => ({}),
  list: async (query) => {
    const p = query as Record<string, any>;
    const res: any = await listMcpTool({keyword: p.keyword, riskLevel: p.riskLevel, limit: 500});
    const records = Array.isArray(res?.data) ? res.data : [];
    return {data: {records, total: records.length}} as R;
  },
  toolbarActions: [
    {
      key: 'refresh-catalog',
      label: t('settings.mcp.refreshCatalog'),
      icon: 'RefreshRight',
      loading: handlers.refreshing,
      onClick: handlers.onRefresh,
    },
  ],
  emptyText: t('settings.mcp.empty'),
});
