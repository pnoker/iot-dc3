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

import {httpGet, httpPost} from '@/api/common';
import {API_MCP_BASE} from '@/config/constant/api';
import type {
  McpAuditRecord,
  McpClientRegistrationForm,
  McpConnectionForm,
  McpConnectionRecord,
  McpToolRecord,
  OAuthClientRecord,
} from '@/config/types/auth';

export const getMcpMetadata = () => httpGet<R<Record<string, unknown>>>(`${API_MCP_BASE}/metadata`);

export const registerMcpClient = (body: McpClientRegistrationForm) =>
  httpPost<R<Record<string, unknown>>>(`${API_MCP_BASE}/client/register`, body);

export const listMcpClient = () => httpPost<R<OAuthClientRecord[]>>(`${API_MCP_BASE}/client/list`);

export const listMcpConnection = () => httpPost<R<McpConnectionRecord[]>>(`${API_MCP_BASE}/connection/list`);

export const addMcpConnection = (body: McpConnectionForm) =>
  httpPost<R<McpConnectionRecord>>(`${API_MCP_BASE}/connection/add`, body);

export const revokeMcpConnection = (id: string) =>
  httpPost<R<boolean>>(`${API_MCP_BASE}/connection/revoke`, undefined, {params: {id}});

export const replaceMcpConnectionTools = (connectionId: string, toolIds: string[]) =>
  httpPost<R<boolean>>(`${API_MCP_BASE}/connection/tools/replace`, {
    connectionId,
    toolIds,
  });

export const listMcpConnectionTool = (id: string) =>
  httpGet<R<string[]>>(`${API_MCP_BASE}/connection/tools/list`, {params: {id}});

export const refreshMcpToolCatalog = () => httpPost<R<number>>(`${API_MCP_BASE}/tool/catalog/refresh`);

export const listMcpTool = (query: { keyword?: string; riskLevel?: string; limit?: number } = {}) =>
  httpPost<R<McpToolRecord[]>>(`${API_MCP_BASE}/tool/list`, query);

export const listMcpAudit = (
  params: {
    principalId?: string;
    toolId?: string;
    status?: string;
    riskLevel?: string;
    limit?: number;
  } = {}
) =>
  httpPost<R<McpAuditRecord[]>>(`${API_MCP_BASE}/audit/list`, undefined, {
    // Wire params follow the platform snake_case query convention; the camelCase
    // signature is kept for callers. Undefined keys are dropped by the transport.
    params: {
      principal_id: params.principalId,
      tool_id: params.toolId,
      status: params.status,
      risk_level: params.riskLevel,
      limit: params.limit,
    },
  });
