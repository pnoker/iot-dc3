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
 * Event definition types (dc3_event / dc3_event_param).
 */

export interface EventRecord {
  id: string;
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
  tenantId?: string;
  createTime?: string;
  operateTime?: string;
  [key: string]: unknown;
}

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

/**
 * Event report history (dc3_event_history).
 */
export interface EventHistory {
  id?: string;
  recordId: string;
  tenantId?: string;
  deviceId?: string;
  eventId?: string;
  eventCode?: string;
  eventTypeFlag?: number;
  eventLevelFlag?: number;
  paramValues?: Record<string, string>;
  configSnapshot?: string;
  message?: string;
  occurTime?: string;
  receiveTime?: string;
  acknowledgeFlag?: number;
  acknowledgeTime?: string;
  acknowledgeUserId?: string;
  schemaVersion?: number;
  createTime?: string;
  operateTime?: string;
  [key: string]: unknown;
}
