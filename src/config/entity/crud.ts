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
 * Shared CRUD entity types consumed by every src/api/*.ts module.
 *
 * Convention:
 *   - <Entity>Form   — shape sent to add/update endpoints (from form data).
 *   - <Entity>Record — shape returned by list/detail endpoints.
 *   - Everything is optional to stay flexible with backend fields; strict
 *     required flags belong in view-layer FormRules, not API signatures.
 */

// ─── User ────────────────────────────────────────────────────────────

export interface UserForm {
  id?: string;
  userName?: string;
  nickName?: string;
  phone?: string;
  email?: string;
  enableFlag?: number;
  [key: string]: unknown;
}

export interface UserRecord extends UserForm {
  id: string;
  createTime?: string;
  updateTime?: string;
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
  updateTime?: string;
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
  titleZh?: string;
  titleEn?: string;
  icon?: string;
  url?: string;
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface MenuRecord extends MenuForm {
  id: string;
  createTime?: string;
  updateTime?: string;
  children?: MenuRecord[];
}

// ─── Resource ────────────────────────────────────────────────────────

export interface ResourceForm {
  id?: string;
  parentResourceId?: number | string;
  resourceName?: string;
  resourceCode?: string;
  resourceTypeFlag?: string;
  entityId?: string | number;
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface ResourceRecord extends ResourceForm {
  id: string;
  createTime?: string;
  updateTime?: string;
  children?: ResourceRecord[];
}

// ─── Api ─────────────────────────────────────────────────────────────

export interface ApiForm {
  id?: string;
  apiName?: string;
  apiCode?: string;
  apiMethod?: string;
  apiUrl?: string;
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface ApiRecord extends ApiForm {
  id: string;
  createTime?: string;
  updateTime?: string;
}

// ─── Device ──────────────────────────────────────────────────────────

export interface DeviceForm {
  id?: string;
  deviceName?: string;
  driverId?: string;
  profileIds?: string[];
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface DeviceRecord extends DeviceForm {
  id: string;
  createTime?: string;
  updateTime?: string;
}

// ─── Driver ──────────────────────────────────────────────────────────

export interface DriverRecord {
  id: string;
  driverName?: string;
  enableFlag?: string;
  createTime?: string;
  updateTime?: string;
  [key: string]: unknown;
}

// ─── Profile ─────────────────────────────────────────────────────────

export interface ProfileForm {
  id?: string;
  profileName?: string;
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface ProfileRecord extends ProfileForm {
  id: string;
  createTime?: string;
  updateTime?: string;
}

// ─── Point ───────────────────────────────────────────────────────────

export interface PointForm {
  id?: string;
  pointName?: string;
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface PointRecord extends PointForm {
  id: string;
  createTime?: string;
  updateTime?: string;
}

// ─── Driver/Point Attribute Info ─────────────────────────────────────

export interface DriverInfoForm {
  id?: string;
  driverId?: string;
  attributeId?: string;
  value?: string;
  [key: string]: unknown;
}

export interface PointInfoForm {
  id?: string;
  pointId?: string;
  attributeId?: string;
  value?: string;
  [key: string]: unknown;
}

// ─── Bind payloads ───────────────────────────────────────────────────

export interface RoleUserBindForm {
  roleId?: string;
  userId?: string;
  [key: string]: unknown;
}

export interface RoleResourceBindForm {
  roleId?: string;
  resourceId?: string;
  [key: string]: unknown;
}
