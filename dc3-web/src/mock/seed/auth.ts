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

import type {
  IdentityAuditRecord,
  LocalCredentialRecord,
  PrincipalRecord,
  RolePrincipalBindRecord,
  RoleRecord,
  RoleResourceBindForm,
  ServiceAccountRecord,
  TenantMembershipRecord,
  UserRecord,
} from '@/config/types/auth';

/** Stable timestamps so the demo does not regenerate on every load. */
const CREATED = '2026-07-15T09:30:00';
const UPDATED = '2026-08-01T14:20:00';
const LAST_LOGIN = '2026-08-06T08:45:00';
const PWD_EXPIRE = '2027-07-15T09:30:00';
const SA_EXPIRE = '2027-08-01T00:00:00';

// ─── Principals ─────────────────────────────────────────────────────
// principalId 201 = dc3 system admin (backs user 101); 204/205 are the two
// service-account principals that own device-data-collector / mqtt-bridge.

interface PrincipalDef {
  name: string;
  type: string;
  displayName: string;
  source: string;
  lastLogin?: string;
}

const principalDefs: PrincipalDef[] = [
  {name: 'dc3', type: 'USER', displayName: '系统管理员', source: 'LOCAL', lastLogin: LAST_LOGIN},
  {name: 'ops', type: 'USER', displayName: '运维工程师', source: 'LOCAL', lastLogin: LAST_LOGIN},
  {name: 'guest', type: 'USER', displayName: '访客用户', source: 'LOCAL'},
  {name: 'device-data-collector', type: 'SERVICE_ACCOUNT', displayName: '设备数据采集', source: 'SYSTEM', lastLogin: LAST_LOGIN},
  {name: 'mqtt-bridge', type: 'SERVICE_ACCOUNT', displayName: 'MQTT桥接接入', source: 'SYSTEM', lastLogin: LAST_LOGIN},
];

export const principals: PrincipalRecord[] = principalDefs.map((p, i) => ({
  id: String(201 + i),
  principalType: p.type,
  principalName: p.name,
  displayName: p.displayName,
  sourceType: p.source,
  enableFlag: 'ENABLE',
  lockedFlag: 'UNLOCKED',
  lastLoginTime: p.lastLogin,
  createTime: CREATED,
  operateTime: UPDATED,
}));

// ─── Users ──────────────────────────────────────────────────────────
// Each user row maps 1:1 to a USER principal above (101↔201, 102↔202, 103↔203).

interface UserDef {
  principalId: string;
  userName: string;
  nickName: string;
  phone: string;
  email: string;
  enabled?: boolean;
}

const userDefs: UserDef[] = [
  {principalId: '201', userName: 'dc3', nickName: '系统管理员', phone: '13800138000', email: 'admin@dc3.site'},
  {principalId: '202', userName: 'ops', nickName: '运维工程师', phone: '13800138001', email: 'ops@dc3.site'},
  {principalId: '203', userName: 'guest', nickName: '访客用户', phone: '13800138002', email: 'guest@dc3.site', enabled: false},
];

export const users: UserRecord[] = userDefs.map((u, i) => ({
  id: String(101 + i),
  principalId: u.principalId,
  userName: u.userName,
  nickName: u.nickName,
  phone: u.phone,
  email: u.email,
  enableFlag: u.enabled === false ? 'DISABLE' : 'ENABLE',
  createTime: CREATED,
  operateTime: UPDATED,
}));

// ─── Roles ──────────────────────────────────────────────────────────
// parentRoleId 0 marks a root. ROLE_OPERATOR_LEAD and ROLE_AUDITOR are
// nested under ROLE_OPERATOR / ROLE_ADMIN to exercise the role tree.

interface RoleDef {
  parentRoleId: number;
  roleName: string;
  roleCode: string;
  remark: string;
  enabled?: boolean;
}

const roleDefs: RoleDef[] = [
  {parentRoleId: 0, roleName: '系统管理员', roleCode: 'ROLE_ADMIN', remark: '内置超级管理员，拥有全部权限'},
  {parentRoleId: 0, roleName: '运维人员', roleCode: 'ROLE_OPERATOR', remark: '负责设备与驱动的日常运维'},
  {parentRoleId: 0, roleName: '只读访客', roleCode: 'ROLE_VIEWER', remark: '仅可查看监控数据'},
  {parentRoleId: 2, roleName: '运维组长', roleCode: 'ROLE_OPERATOR_LEAD', remark: '运维组长，可管理运维人员'},
  {parentRoleId: 1, roleName: '审计员', roleCode: 'ROLE_AUDITOR', remark: '审计员，查看审计与日志', enabled: false},
];

export const roles: RoleRecord[] = roleDefs.map((r, i) => ({
  id: String(1 + i),
  parentRoleId: r.parentRoleId,
  roleName: r.roleName,
  roleCode: r.roleCode,
  enableFlag: r.enabled === false ? 'DISABLE' : 'ENABLE',
  remark: r.remark,
  createTime: CREATED,
  operateTime: UPDATED,
}));

// ─── Role ↔ Principal binds ─────────────────────────────────────────
// dc3 (201) ↔ ROLE_ADMIN; service-account principals (204/205) ↔ operational roles.

interface RolePrincipalBindDef {
  roleId: string;
  principalId: string;
  principalType: string;
}

const rolePrincipalBindDefs: RolePrincipalBindDef[] = [
  {roleId: '1', principalId: '201', principalType: 'USER'},
  {roleId: '2', principalId: '202', principalType: 'USER'},
  {roleId: '3', principalId: '203', principalType: 'USER'},
  {roleId: '2', principalId: '204', principalType: 'SERVICE_ACCOUNT'},
  {roleId: '3', principalId: '205', principalType: 'SERVICE_ACCOUNT'},
];

export const rolePrincipalBinds: RolePrincipalBindRecord[] = rolePrincipalBindDefs.map((b, i) => ({
  id: String(1 + i),
  roleId: b.roleId,
  principalId: b.principalId,
  principalType: b.principalType,
  createTime: CREATED,
}));

// ─── Role ↔ Resource binds ──────────────────────────────────────────
// resourceId 5001+ aligns with the manager-seed resource namespace.

interface RoleResourceBindDef {
  roleId: string;
  resourceId: string;
}

const roleResourceBindDefs: RoleResourceBindDef[] = [
  {roleId: '1', resourceId: '5001'},
  {roleId: '1', resourceId: '5002'},
  {roleId: '2', resourceId: '5003'},
  {roleId: '2', resourceId: '5004'},
  {roleId: '3', resourceId: '5005'},
];

export const roleResourceBinds: (RoleResourceBindForm & {id: string; createTime: string})[] =
  roleResourceBindDefs.map((b, i) => ({
    id: String(1 + i),
    roleId: b.roleId,
    resourceId: b.resourceId,
    createTime: CREATED,
  }));

// ─── Service accounts ───────────────────────────────────────────────
// Both service accounts are owned by the dc3 admin (principal 201).

interface ServiceAccountDef {
  principalId: string;
  serviceAccountName: string;
  purpose: string;
}

const serviceAccountDefs: ServiceAccountDef[] = [
  {principalId: '204', serviceAccountName: 'device-data-collector', purpose: '采集驱动上报位号数据'},
  {principalId: '205', serviceAccountName: 'mqtt-bridge', purpose: 'MQTT 设备接入与协议桥接'},
];

export const serviceAccounts: ServiceAccountRecord[] = serviceAccountDefs.map((s, i) => ({
  id: String(1 + i),
  principalId: s.principalId,
  serviceAccountName: s.serviceAccountName,
  ownerPrincipalId: '201',
  purpose: s.purpose,
  expireTime: SA_EXPIRE,
  enableFlag: 'ENABLE',
  createTime: CREATED,
  operateTime: UPDATED,
}));

// ─── Tenant memberships ─────────────────────────────────────────────
// Every principal above belongs to the default tenant as an ACTIVE member.

interface TenantMembershipDef {
  principalId: string;
  principalType: string;
}

const tenantMembershipDefs: TenantMembershipDef[] = [
  {principalId: '201', principalType: 'USER'},
  {principalId: '202', principalType: 'USER'},
  {principalId: '203', principalType: 'USER'},
  {principalId: '204', principalType: 'SERVICE_ACCOUNT'},
  {principalId: '205', principalType: 'SERVICE_ACCOUNT'},
];

export const tenantMemberships: TenantMembershipRecord[] = tenantMembershipDefs.map((m, i) => ({
  id: String(1 + i),
  tenantId: 'default',
  principalId: m.principalId,
  principalType: m.principalType,
  membershipStatus: 'ACTIVE',
  joinedTime: CREATED,
  createTime: CREATED,
}));

// ─── Identity audits ────────────────────────────────────────────────
// A login lifecycle for dc3 (201), a failed guest (203) login, and an admin
// enabling the guest principal — covers LOGIN/LOGOUT/TOKEN_REFRESH/ENABLE.

interface IdentityAuditDef {
  principalId: string;
  principalType: string;
  action: string;
  resourceType: string;
  resourceId: string;
  resourceName: string;
  status: string;
  errorCode?: string;
  createTime: string;
}

const identityAuditDefs: IdentityAuditDef[] = [
  {
    principalId: '201',
    principalType: 'USER',
    action: 'LOGIN',
    resourceType: 'SESSION',
    resourceId: 'session-201',
    resourceName: 'dc3-web',
    status: 'SUCCESS',
    createTime: '2026-08-06T08:45:00',
  },
  {
    principalId: '201',
    principalType: 'USER',
    action: 'TOKEN_REFRESH',
    resourceType: 'SESSION',
    resourceId: 'session-201',
    resourceName: 'dc3-web',
    status: 'SUCCESS',
    createTime: '2026-08-06T09:10:00',
  },
  {
    principalId: '203',
    principalType: 'USER',
    action: 'LOGIN',
    resourceType: 'SESSION',
    resourceId: 'session-203',
    resourceName: 'dc3-web',
    status: 'FAILURE',
    errorCode: 'BAD_CREDENTIALS',
    createTime: '2026-08-05T22:30:00',
  },
  {
    principalId: '201',
    principalType: 'USER',
    action: 'LOGOUT',
    resourceType: 'SESSION',
    resourceId: 'session-201',
    resourceName: 'dc3-web',
    status: 'SUCCESS',
    createTime: '2026-08-05T18:00:00',
  },
  {
    principalId: '201',
    principalType: 'USER',
    action: 'ENABLE',
    resourceType: 'PRINCIPAL',
    resourceId: '203',
    resourceName: 'guest',
    status: 'SUCCESS',
    createTime: '2026-07-20T11:00:00',
  },
];

export const identityAudits: IdentityAuditRecord[] = identityAuditDefs.map((a, i) => ({
  id: String(1 + i),
  tenantId: 'default',
  principalId: a.principalId,
  principalType: a.principalType,
  action: a.action,
  resourceType: a.resourceType,
  resourceId: a.resourceId,
  resourceName: a.resourceName,
  status: a.status,
  errorCode: a.errorCode,
  createTime: a.createTime,
}));

// ─── Local credentials ──────────────────────────────────────────────
// guest (203) carries failedAttempts: 3 to mirror its failed LOGIN audit above.

interface LocalCredentialDef {
  principalId: string;
  loginName: string;
  failedAttempts: number;
}

const localCredentialDefs: LocalCredentialDef[] = [
  {principalId: '201', loginName: 'dc3', failedAttempts: 0},
  {principalId: '202', loginName: 'ops', failedAttempts: 0},
  {principalId: '203', loginName: 'guest', failedAttempts: 3},
];

export const localCredentials: LocalCredentialRecord[] = localCredentialDefs.map((c, i) => ({
  id: String(1 + i),
  principalId: c.principalId,
  loginName: c.loginName,
  credentialType: 'PASSWORD',
  enableFlag: 'ENABLE',
  passwordUpdatedTime: CREATED,
  passwordExpireTime: PWD_EXPIRE,
  failedAttempts: c.failedAttempts,
  createTime: CREATED,
}));
