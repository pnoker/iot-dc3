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

export const listMcpTool = (query: {keyword?: string; riskLevel?: string; limit?: number} = {}) =>
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
