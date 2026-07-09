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

// ─── Local Credential ────────────────────────────────────────────────

export interface LocalCredentialForm {
  id?: string;
  principalId?: string;
  loginName?: string;
  password?: string;
  enableFlag?: string;

  [key: string]: unknown;
}

export interface LocalCredentialRecord extends LocalCredentialForm {
  id: string;
  credentialType?: string;
  passwordUpdatedTime?: string;
  passwordExpireTime?: string;
  failedAttempts?: number;
  createTime?: string;
  operateTime?: string;
}

// ─── Tenant Membership ───────────────────────────────────────────────

export interface TenantMembershipForm {
  id?: string;
  tenantId?: string;
  principalId?: string;
  principalType?: 'USER' | 'SERVICE_ACCOUNT' | 'SYSTEM' | string;
  membershipStatus?: 'ACTIVE' | 'SUSPENDED' | 'INVITED' | string;

  [key: string]: unknown;
}

export interface TenantMembershipRecord extends TenantMembershipForm {
  id: string;
  joinedTime?: string;
  createTime?: string;
  operateTime?: string;
}

// ─── Principal ───────────────────────────────────────────────────────

export interface PrincipalForm {
  id?: string;
  principalType?: 'USER' | 'SERVICE_ACCOUNT' | 'SYSTEM' | string;
  principalName?: string;
  displayName?: string;
  sourceType?: string;
  enableFlag?: string;
  lockedFlag?: string;

  [key: string]: unknown;
}

export interface PrincipalRecord extends PrincipalForm {
  id: string;
  lastLoginTime?: string;
  createTime?: string;
  operateTime?: string;
}

export interface RolePrincipalBindForm {
  roleId?: string;
  principalId?: string;
  principalType?: 'USER' | 'SERVICE_ACCOUNT' | 'SYSTEM' | string;

  [key: string]: unknown;
}

export interface RolePrincipalBindRecord extends RolePrincipalBindForm {
  id: string;
  createTime?: string;
  operateTime?: string;
}

export interface RoleResourceBindForm {
  roleId?: string;
  resourceId?: string;

  [key: string]: unknown;
}

// ─── Service Account ────────────────────────────────────────────────

export interface ServiceAccountForm {
  id?: string;
  principalId?: string;
  serviceAccountName?: string;
  ownerPrincipalId?: string;
  purpose?: string;
  expireTime?: string;
  enableFlag?: string;

  [key: string]: unknown;
}

export interface ServiceAccountRecord extends ServiceAccountForm {
  id: string;
  tenantId?: string;
  lastUsedTime?: string;
  createTime?: string;
  operateTime?: string;
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

export interface McpAuditRecord {
  id: string;
  traceId?: string;
  tenantId?: string;
  principalId?: string;
  principalType?: string;
  clientId?: string;
  connectionId?: string;
  toolId?: string;
  toolName?: string;
  permissionCode?: string;
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | string;
  confirmId?: string;
  idempotencyKey?: string;
  argumentDigest?: string;
  status?: string;
  errorCode?: string;
  durationMs?: number;
  clientName?: string;
  clientVersion?: string;
  remoteIp?: string;
  createTime?: string;

  [key: string]: unknown;
}

export interface IdentityAuditRecord {
  id: string;
  tenantId?: string;
  principalId?: string;
  principalType?: string;
  action?: string;
  resourceType?: string;
  resourceId?: string;
  resourceName?: string;
  status?: string;
  errorCode?: string;
  createTime?: string;

  [key: string]: unknown;
}
