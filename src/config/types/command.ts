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
 * Command definition types (dc3_command / dc3_command_param).
 */

export interface CommandForm {
  id?: string;
  commandName?: string;
  commandCode?: string;
  commandTypeFlag?: string | number;
  callTypeFlag?: string | number;
  timeout?: number;
  commandExt?: Record<string, unknown>;
  profileId?: string;
  enableFlag?: string | number;
  signature?: string;
  version?: number;
  remark?: string;
  [key: string]: unknown;
}

export interface CommandRecord extends CommandForm {
  id: string;
  tenantId?: string;
  createTime?: string;
  operateTime?: string;
}

export interface CommandParamForm {
  id?: string;
  paramName?: string;
  paramCode?: string;
  paramDirectionFlag?: string | number;
  paramTypeFlag?: string | number;
  requiredFlag?: boolean;
  defaultValue?: string;
  paramExt?: Record<string, unknown>;
  commandId?: string;
  enableFlag?: string | number;
  signature?: string;
  version?: number;
  [key: string]: unknown;
}

export interface CommandParamRecord extends CommandParamForm {
  id: string;
}

/**
 * Command status (matches backend PointCommandStatusEnum constant names).
 * Backend serializes enums by name, so the wire value is one of these strings.
 */
export type CommandStatus = 'PENDING' | 'SENT' | 'SUCCESS' | 'FAILED' | 'TIMEOUT' | 'EXPIRED' | 'DEAD' | 'DUPLICATE';

/**
 * Command call history (dc3_command_history).
 */
export interface CommandHistoryRecord {
  id?: string;
  recordId: string;
  tenantId?: string;
  deviceId?: string;
  commandId?: string;
  commandCode?: string;
  paramValues?: Record<string, string>;
  status?: CommandStatus;
  errorCode?: string;
  errorMessage?: string;
  resultValues?: Record<string, string>;
  configSnapshot?: string;
  source?: string;
  sourceUserId?: string;
  occurTime?: string;
  sendTime?: string;
  finishTime?: string;
  expireTime?: string;
  schemaVersion?: number;
  createTime?: string;
  operateTime?: string;
  [key: string]: unknown;
}
