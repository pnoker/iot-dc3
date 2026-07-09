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
 * Manager-domain entity types (dc3-common-manager).
 */

// ─── Device ──────────────────────────────────────────────────────────

export interface DeviceExt {
  content?: {
    keep?: string;
  };
}

export interface DeviceForm {
  id?: string;
  deviceName?: string;
  deviceCode?: string;
  driverId?: string;
  profileId?: string;
  deviceExt?: DeviceExt;
  enableFlag?: string;
  signature?: string;
  version?: number;
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

export interface ProfileExt {
  content?: {
    keep?: string;
  };
}

export interface ProfileForm {
  id?: string;
  profileName?: string;
  profileCode?: string;
  profileShareFlag?: string;
  profileTypeFlag?: string;
  profileExt?: ProfileExt;
  enableFlag?: string;
  signature?: string;
  version?: number;
  remark?: string;

  [key: string]: unknown;
}

export interface ProfileRecord extends ProfileForm {
  id: string;
  createTime?: string;
  operateTime?: string;
}

// ─── Point ───────────────────────────────────────────────────────────

export interface PointExt {
  content?: {
    keep?: string;
  };
}

export interface PointForm {
  id?: string;
  pointName?: string;
  pointCode?: string;
  pointTypeFlag?: string;
  rwFlag?: string;
  baseValue?: number | string;
  multiple?: number | string;
  valueDecimal?: number | string;
  unit?: string;
  profileId?: string;
  pointExt?: PointExt;
  enableFlag?: string;
  signature?: string;
  version?: number;
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

export interface CommandInfoForm {
  id?: string;
  deviceId?: string;
  commandId?: string;
  attributeId?: string;
  configValue?: string;

  [key: string]: unknown;
}

export interface EventInfoForm {
  id?: string;
  deviceId?: string;
  eventId?: string;
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
