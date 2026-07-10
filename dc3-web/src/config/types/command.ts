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
 * Command source (matches backend CommandHistorySourceEnum constant names).
 * Backend serializes enums by name, so the wire value is one of these strings.
 */
export type CommandSource = 'HTTP' | 'GRPC' | 'AGENTIC';

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
  source?: CommandSource;
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
