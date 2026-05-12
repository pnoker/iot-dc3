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
 * Manager-domain entity types (dc3-common-manager).
 */

// ─── Device ──────────────────────────────────────────────────────────

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

// ─── Attribute Config ────────────────────────────────────────────────

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

// ─── Group / Label ──────────────────────────────────────────────────

export interface GroupForm {
  id?: string;
  parentGroupId?: number | string | null;
  groupTypeFlag?: string;
  groupName?: string;
  groupCode?: string;
  groupLevel?: number;
  groupIndex?: number;
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface GroupRecord extends GroupForm {
  id: string;
  createTime?: string;
  operateTime?: string;
  children?: GroupRecord[];
}

export interface LabelForm {
  id?: string;
  labelName?: string;
  labelCode?: string;
  labelColor?: string;
  entityTypeFlag?: string;
  enableFlag?: string;
  remark?: string;
  [key: string]: unknown;
}

export interface LabelRecord extends LabelForm {
  id: string;
  createTime?: string;
  operateTime?: string;
}
