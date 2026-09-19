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
import type {PageQuery, PageResult} from '@/config/types';
import type {
  McpAuditRecord,
  McpClientRegistrationForm,
  McpConnectionForm,
  McpConnectionRecord,
  McpMetadata,
  McpToolRecord,
  OAuthClientRecord,
} from '@/config/types/auth';

/**
 * Fetch mcp metadata.
 * @returns the fetched mcp metadata
 */
export const getMcpMetadata = () => httpGet<McpMetadata>(`${API_MCP_BASE}/metadata`);

/**
 * Register mcp client.
 * @param body - request body payload
 * @returns the record response
 */
export const registerMcpClient = (body: McpClientRegistrationForm) =>
  httpPost<Record<string, unknown>>(`${API_MCP_BASE}/client/register`, body);

/**
 * List mcp client.
 * @returns the listed mcp client
 */
export const listMcpClient = () => httpPost<OAuthClientRecord[]>(`${API_MCP_BASE}/client/list`);

/**
 * List mcp connection.
 * @returns the listed mcp connection
 */
export const listMcpConnection = () => httpPost<McpConnectionRecord[]>(`${API_MCP_BASE}/connection/list`);

/**
 * Create mcp connection.
 * @param body - request body payload
 * @returns the operation result
 */
export const addMcpConnection = (body: McpConnectionForm) =>
  httpPost<McpConnectionRecord>(`${API_MCP_BASE}/connection/add`, body);

/**
 * Revoke an MCP connection.
 * @param id - record id
 * @returns the boolean response
 */
export const revokeMcpConnection = (id: string) =>
  httpPost<boolean>(`${API_MCP_BASE}/connection/revoke`, undefined, {params: {id}});

/**
 * Replace the tools bound to an MCP connection.
 * @param connectionId - connection id to scope the request
 * @param toolIds - tool ids to scope the request
 * @returns the boolean response
 */
export const replaceMcpConnectionTools = (connectionId: string, toolIds: string[]) =>
  httpPost<boolean>(`${API_MCP_BASE}/connection/tools/replace`, {
    connectionId,
    toolIds,
  });

/**
 * List mcp connection tool.
 * @param id - record id
 * @returns the listed mcp connection tool
 */
export const listMcpConnectionTool = (id: string) =>
  httpGet<string[]>(`${API_MCP_BASE}/connection/tools/list`, {params: {id}});

/**
 * Refresh the MCP tool catalog.
 * @returns the refresh result
 */
export const refreshMcpToolCatalog = () => httpPost<number>(`${API_MCP_BASE}/tool/catalog/refresh`);

/**
 * List mcp tool.
 * @param query - page query with filters and paging
 * @param query.keyword - search keyword filter
 * @param query.riskLevel - risk level filter
 * @param query.offset - page offset
 * @param query.limit - page size
 * @param query.sort - sort field and direction
 * @returns the listed mcp tool
 */
export const listMcpTool = (query: {
  keyword?: string;
  riskLevel?: string;
  offset?: number;
  limit?: number;
  sort?: PageQuery['sort'];
} = {}) => httpPost<PageResult<McpToolRecord>>(`${API_MCP_BASE}/tool/list`, query);

/**
 * List mcp audit.
 * @param params - query parameters for the request
 * @param params.principalId - principal id to scope the request
 * @param params.toolId - tool id to scope the request
 * @param params.status - status entries
 * @param params.riskLevel - risk level filter
 * @param params.offset - page offset
 * @param params.limit - maximum number of entries to return
 * @returns the listed mcp audit
 */
export const listMcpAudit = (
  params: {
    principalId?: string;
    toolId?: string;
    status?: string;
    riskLevel?: string;
    offset?: number;
    limit?: number;
  } = {}
) =>
  httpPost<PageResult<McpAuditRecord>>(`${API_MCP_BASE}/audit/list`, undefined, {
    // Wire params follow the platform snake_case query convention; the camelCase
    // signature is kept for callers. Undefined keys are dropped by the transport.
    params: {
      principal_id: params.principalId,
      tool_id: params.toolId,
      status: params.status,
      risk_level: params.riskLevel,
      offset: params.offset,
      limit: params.limit,
    },
  });
