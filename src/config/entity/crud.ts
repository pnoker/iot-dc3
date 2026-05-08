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
 *   - <Entity>Form   — shape sent to add/update endpoints (matches backend VO).
 *   - <Entity>Record — shape returned by list/detail endpoints.
 *   - Everything is optional to stay flexible with backend fields; strict
 *     required flags belong in view-layer FormRules, not API signatures.
 *
 * Backend reference: dc3-common-{auth,manager}/.../entity/vo/*VO.java
 * All VOs extend BaseVO: id, remark, creatorId, creatorName, createTime,
 * operatorId, operatorName, operateTime.
 */

// ─── User ────────────────────────────────────────────────────────────
// Backend: UserVO (dc3-common-auth)

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
  operateTime?: string;
}

// ─── Role ────────────────────────────────────────────────────────────
// Backend: RoleVO (dc3-common-auth)

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
// Backend: MenuVO (dc3-common-auth)
// MenuExt.content carries titles (zh/en), icon, url as a nested object.

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
// Backend: ResourceVO (dc3-common-auth)

export interface ResourceForm {
  id?: string;
  parentResourceId?: number | string;
  resourceName?: string;
  resourceCode?: string;
  resourceTypeFlag?: string;
  resourceScopeFlag?: string;
  entityId?: string | number;
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
// Backend: ApiVO (dc3-common-auth)
// ApiExt.content carries url, title, remark as a nested object.

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

// ─── Device ──────────────────────────────────────────────────────────
// Backend: DeviceVO (dc3-common-manager)

export interface DeviceForm {
  id?: string;
  deviceName?: string;
  deviceCode?: string;
  driverId?: string;
  profileIds?: string[];
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface DeviceRecord extends DeviceForm {
  id: string;
  createTime?: string;
  operateTime?: string;
}

// ─── Driver ──────────────────────────────────────────────────────────
// Backend: DriverVO (dc3-common-manager)

export interface DriverRecord {
  id: string;
  driverName?: string;
  driverCode?: string;
  serviceName?: string;
  serviceHost?: string;
  driverTypeFlag?: string;
  enableFlag?: string;
  createTime?: string;
  operateTime?: string;
  [key: string]: unknown;
}

// ─── Profile ─────────────────────────────────────────────────────────
// Backend: ProfileVO (dc3-common-manager)

export interface ProfileForm {
  id?: string;
  profileName?: string;
  profileCode?: string;
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface ProfileRecord extends ProfileForm {
  id: string;
  createTime?: string;
  operateTime?: string;
}

// ─── Point ───────────────────────────────────────────────────────────
// Backend: PointVO (dc3-common-manager)

export interface PointForm {
  id?: string;
  pointName?: string;
  pointCode?: string;
  pointTypeFlag?: string;
  profileId?: string;
  unit?: string;
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface PointRecord extends PointForm {
  id: string;
  createTime?: string;
  operateTime?: string;
}

// ─── Driver/Point Attribute Config ───────────────────────────────────
// Backend: DriverAttributeConfigVO / PointAttributeConfigVO
// These are per-device attribute configurations, not per-driver.

export interface DriverInfoForm {
  id?: string;
  deviceId?: string;
  attributeId?: string;
  configValue?: string;
  [key: string]: unknown;
}

export interface PointInfoForm {
  id?: string;
  deviceId?: string;
  pointId?: string;
  attributeId?: string;
  configValue?: string;
  [key: string]: unknown;
}

// ─── Bind payloads ───────────────────────────────────────────────────
// Backend: RoleUserBindVO / RoleResourceBindVO (dc3-common-auth)

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
