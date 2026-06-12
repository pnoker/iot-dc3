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

/**
 * Auth-domain entity types (dc3-common-auth).
 */

// ─── User ────────────────────────────────────────────────────────────

export interface UserForm {
  id?: string;
  principalId?: string;
  userName?: string;
  nickName?: string;
  phone?: string;
  email?: string;
  enableFlag?: string;
  [key: string]: unknown;
}

export interface UserRecord extends UserForm {
  id: string;
  createTime?: string;
  operateTime?: string;
}

// ─── Role ────────────────────────────────────────────────────────────

export interface RoleForm {
  id?: string;
  parentRoleId?: number | string;
  roleName?: string;
  roleCode?: string;
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface RoleRecord extends RoleForm {
  id: string;
  createTime?: string;
  operateTime?: string;
  children?: RoleRecord[];
}

// ─── Menu ────────────────────────────────────────────────────────────

export interface MenuForm {
  id?: string;
  parentMenuId?: number | string;
  menuName?: string;
  menuCode?: string;
  menuTypeFlag?: string;
  menuLevel?: string;
  menuIndex?: number;
  enableFlag?: string;
  remark?: string;
  menuExt?: {
    content?: {
      titles?: Record<string, string>;
      icon?: string;
      url?: string;
    };
  };
  [key: string]: unknown;
}

export interface MenuRecord extends MenuForm {
  id: string;
  createTime?: string;
  operateTime?: string;
  children?: MenuRecord[];
}

// ─── Resource ────────────────────────────────────────────────────────

export interface ResourceForm {
  id?: string;
  parentResourceId?: number | string;
  resourceName?: string;
  resourceCode?: string;
  serviceName?: string;
  resourceTypeFlag?: string;
  resourceScopeFlag?: string;
  entityId?: string | number;
  resourceExt?: Record<string, unknown>;
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface ResourceRecord extends ResourceForm {
  id: string;
  createTime?: string;
  operateTime?: string;
  children?: ResourceRecord[];
}

// ─── Api ─────────────────────────────────────────────────────────────

export interface ApiForm {
  id?: string;
  apiName?: string;
  apiCode?: string;
  serviceName?: string;
  apiTypeFlag?: string;
  apiGroup?: string;
  enableFlag?: string;
  remark?: string;
  apiExt?: {
    content?: {
      url?: string;
      title?: string;
      remark?: string;
    };
  };
  [key: string]: unknown;
}

export interface ApiRecord extends ApiForm {
  id: string;
  createTime?: string;
  operateTime?: string;
}

// ─── Bind payloads ───────────────────────────────────────────────────

export interface RolePrincipalBindForm {
  roleId?: string;
  principalId?: string;
  principalType?: 'USER' | 'SERVICE_ACCOUNT' | 'SYSTEM' | string;
  [key: string]: unknown;
}

export interface RoleResourceBindForm {
  roleId?: string;
  resourceId?: string;
  [key: string]: unknown;
}

// ─── MCP / OAuth ────────────────────────────────────────────────────

export interface McpClientRegistrationForm {
  client_name?: string;
  client_type?: 'PUBLIC' | 'CONFIDENTIAL' | string;
  grant_types?: string[];
  redirect_uris?: string[];
  scope?: string[];
  tenant_id?: string;
  service_account_principal_id?: string;
  [key: string]: unknown;
}

export interface OAuthClientRecord {
  id: string;
  clientId: string;
  clientName: string;
  clientType: string;
  ownerPrincipalId?: string;
  serviceAccountPrincipalId?: string;
  tenantId?: string;
  authorizationGrantTypes?: string;
  redirectUris?: string;
  scopes?: string;
  enableFlag?: string | number;
  [key: string]: unknown;
}

export interface McpConnectionForm {
  connectionName?: string;
  clientId?: string;
  principalId?: string;
  principalType?: 'USER' | 'SERVICE_ACCOUNT' | string;
  tenantId?: string;
  grantType?: 'authorization_code' | 'client_credentials' | string;
  expireTime?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface McpConnectionRecord extends McpConnectionForm {
  id: string;
  enableFlag?: string | number;
  revokeTime?: string;
  lastUsedTime?: string;
}

export interface McpToolRecord {
  id: string;
  toolId: string;
  toolName: string;
  toolTitle?: string;
  toolCategory?: string;
  serviceName?: string;
  apiCode?: string;
  permissionCode?: string;
  httpMethod?: string;
  apiPath?: string;
  schemaHash?: string;
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | string;
  readOnlyHint?: number;
  destructiveHint?: number;
  idempotentHint?: number;
  openWorldHint?: number;
  enableFlag?: string | number;
  remark?: string;
  [key: string]: unknown;
}
