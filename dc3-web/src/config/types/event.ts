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
 * Event definition types (dc3_event / dc3_event_param).
 */

export interface EventForm {
  id?: string;
  eventName?: string;
  eventCode?: string;
  eventTypeFlag?: string | number;
  eventLevelFlag?: string | number;
  eventExt?: Record<string, unknown>;
  profileId?: string;
  enableFlag?: string | number;
  signature?: string;
  version?: number;
  remark?: string;

  [key: string]: unknown;
}

export interface EventRecord extends EventForm {
  id: string;
  tenantId?: string;
  createTime?: string;
  operateTime?: string;
}

export interface EventParamForm {
  id?: string;
  paramName?: string;
  paramCode?: string;
  paramTypeFlag?: string | number;
  paramExt?: Record<string, unknown>;
  eventId?: string;
  enableFlag?: string | number;
  signature?: string;
  version?: number;

  [key: string]: unknown;
}

export interface EventParamRecord extends EventParamForm {
  id: string;
}

/**
 * Event report history (dc3_event_history).
 */
export interface EventHistoryRecord {
  id?: string;
  recordId: string;
  tenantId?: string;
  deviceId?: string;
  eventId?: string;
  eventCode?: string;
  // Backend serializes these as domain enum names (e.g. "ALERT", "HIGH", "NO").
  // Kept as string | number for backward compatibility with older numeric payloads.
  eventTypeFlag?: string | number;
  eventLevelFlag?: string | number;
  paramValues?: Record<string, string>;
  configSnapshot?: string;
  message?: string;
  occurTime?: string;
  receiveTime?: string;
  acknowledgeFlag?: string | number;
  acknowledgeTime?: string;
  acknowledgeUserId?: string;
  schemaVersion?: number;
  createTime?: string;
  operateTime?: string;

  [key: string]: unknown;
}
