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
 * Shared enum option lists mirrored from backend enums (dc3-common-constant).
 * Keep label/value in sync with the Java side so requests serialise cleanly.
 */

export interface EnumOption {
  label: string;
  value: string;
}

export const RESOURCE_TYPE_OPTIONS: EnumOption[] = [
  {label: 'DRIVER', value: 'DRIVER'},
  {label: 'PROFILE', value: 'PROFILE'},
  {label: 'POINT', value: 'POINT'},
  {label: 'DEVICE', value: 'DEVICE'},
  {label: 'DATA', value: 'DATA'},
  {label: 'MENU', value: 'MENU'},
  {label: 'API', value: 'API'},
];

export const RESOURCE_SCOPE_OPTIONS: EnumOption[] = [
  {label: 'ADD', value: 'ADD'},
  {label: 'DELETE', value: 'DELETE'},
  {label: 'UPDATE', value: 'UPDATE'},
  {label: 'LIST', value: 'LIST'},
];

export const API_TYPE_OPTIONS: EnumOption[] = [
  {label: 'GET', value: 'GET'},
  {label: 'POST', value: 'POST'},
  {label: 'PUT', value: 'PUT'},
  {label: 'DELETE', value: 'DELETE'},
];

export const ENABLE_FLAG_OPTIONS: EnumOption[] = [
  {label: 'ENABLE', value: 'ENABLE'},
  {label: 'DISABLE', value: 'DISABLE'},
];

export const ENTITY_TYPE_OPTIONS: EnumOption[] = [
  {label: 'DRIVER', value: 'DRIVER'},
  {label: 'PROFILE', value: 'PROFILE'},
  {label: 'POINT', value: 'POINT'},
  {label: 'DEVICE', value: 'DEVICE'},
];

export const MENU_TYPE_OPTIONS: EnumOption[] = [
  {label: 'TITLE', value: 'TITLE'},
  {label: 'COMMON', value: 'COMMON'},
];

export const MENU_LEVEL_OPTIONS: EnumOption[] = [
  {label: 'ROOT', value: 'ROOT'},
  {label: 'C1', value: 'C1'},
  {label: 'C2', value: 'C2'},
  {label: 'C3', value: 'C3'},
  {label: 'C4', value: 'C4'},
];

// Backend: DriverTypeEnum
export const DRIVER_TYPE_OPTIONS: EnumOption[] = [
  {label: 'DRIVER_CLIENT', value: 'DRIVER_CLIENT'},
  {label: 'DRIVER_SERVER', value: 'DRIVER_SERVER'},
  {label: 'GATEWAY', value: 'GATEWAY'},
  {label: 'CONNECT', value: 'CONNECT'},
];

// Backend: ProfileTypeEnum
export const PROFILE_TYPE_OPTIONS: EnumOption[] = [
  {label: 'SYSTEM', value: 'SYSTEM'},
  {label: 'DRIVER', value: 'DRIVER'},
  {label: 'USER', value: 'USER'},
];

// Backend: PointTypeEnum
export const POINT_TYPE_OPTIONS: EnumOption[] = [
  {label: 'STRING', value: 'STRING'},
  {label: 'BYTE', value: 'BYTE'},
  {label: 'SHORT', value: 'SHORT'},
  {label: 'INT', value: 'INT'},
  {label: 'LONG', value: 'LONG'},
  {label: 'FLOAT', value: 'FLOAT'},
  {label: 'DOUBLE', value: 'DOUBLE'},
  {label: 'BOOLEAN', value: 'BOOLEAN'},
];

// Backend: ProfileShareTypeEnum
export const PROFILE_SHARE_OPTIONS: EnumOption[] = [
  {label: 'TENANT', value: 'TENANT'},
  {label: 'DRIVER', value: 'DRIVER'},
  {label: 'USER', value: 'USER'},
];

// Backend: RwTypeEnum
export const RW_FLAG_OPTIONS: EnumOption[] = [
  {label: 'R', value: 'READ_ONLY'},
  {label: 'W', value: 'WRITE_ONLY'},
  {label: 'RW', value: 'READ_WRITE'},
];

// Backend: AlarmTypeEnum
export const ALARM_TYPE_OPTIONS: EnumOption[] = [
  {label: 'RULE', value: 'RULE'},
  {label: 'OFFLINE', value: 'OFFLINE'},
  {label: 'FAULT', value: 'FAULT'},
  {label: 'STATE_FLIP', value: 'STATE_FLIP'},
  {label: 'REPORT', value: 'REPORT'},
];

// Backend: CommandTypeEnum
export const COMMAND_TYPE_OPTIONS: EnumOption[] = [
  {label: 'CUSTOM', value: 'CUSTOM'},
  {label: 'CONFIG', value: 'CONFIG'},
  {label: 'ACTION', value: 'ACTION'},
];

// Backend: CallTypeEnum
export const CALL_TYPE_OPTIONS: EnumOption[] = [
  {label: 'SYNC', value: 'SYNC'},
  {label: 'ASYNC', value: 'ASYNC'},
];

// Backend: ParamDirectionTypeEnum
export const PARAM_DIRECTION_OPTIONS: EnumOption[] = [
  {label: 'INPUT', value: 'INPUT'},
  {label: 'OUTPUT', value: 'OUTPUT'},
];

// Backend: EventTypeFlagEnum
export const EVENT_TYPE_OPTIONS: EnumOption[] = [
  {label: 'INFO', value: 'INFO'},
  {label: 'ALERT', value: 'ALERT'},
  {label: 'FAULT', value: 'FAULT'},
  {label: 'LIFECYCLE', value: 'LIFECYCLE'},
];

// Backend: EventLevelEnum
export const EVENT_LEVEL_OPTIONS: EnumOption[] = [
  {label: 'LOW', value: 'LOW'},
  {label: 'MEDIUM', value: 'MEDIUM'},
  {label: 'HIGH', value: 'HIGH'},
  {label: 'CRITICAL', value: 'CRITICAL'},
];

// Backend: OAuth registered client type
export const MCP_CLIENT_TYPES = {
  PUBLIC: 'PUBLIC',
  CONFIDENTIAL: 'CONFIDENTIAL',
} as const;

export const MCP_CLIENT_TYPE_OPTIONS: EnumOption[] = [
  {label: MCP_CLIENT_TYPES.PUBLIC, value: MCP_CLIENT_TYPES.PUBLIC},
  {label: MCP_CLIENT_TYPES.CONFIDENTIAL, value: MCP_CLIENT_TYPES.CONFIDENTIAL},
];

// Backend: OAuth grant type
export const MCP_GRANT_TYPES = {
  AUTHORIZATION_CODE: 'authorization_code',
  CLIENT_CREDENTIALS: 'client_credentials',
} as const;

export const MCP_GRANT_TYPE_OPTIONS: EnumOption[] = [
  {label: MCP_GRANT_TYPES.AUTHORIZATION_CODE, value: MCP_GRANT_TYPES.AUTHORIZATION_CODE},
  {label: MCP_GRANT_TYPES.CLIENT_CREDENTIALS, value: MCP_GRANT_TYPES.CLIENT_CREDENTIALS},
];

// Backend: PrincipalTypeEnum
export const MCP_PRINCIPAL_TYPES = {
  USER: 'USER',
  SERVICE_ACCOUNT: 'SERVICE_ACCOUNT',
} as const;

export const MCP_PRINCIPAL_TYPE_OPTIONS: EnumOption[] = [
  {label: MCP_PRINCIPAL_TYPES.USER, value: MCP_PRINCIPAL_TYPES.USER},
  {label: MCP_PRINCIPAL_TYPES.SERVICE_ACCOUNT, value: MCP_PRINCIPAL_TYPES.SERVICE_ACCOUNT},
];

// Backend: McpConstant.Scope
export const MCP_SCOPES = {
  TOOLS_LIST: 'mcp:tools:list',
  TOOLS_CALL: 'mcp:tools:call',
  TOOLS_CALL_HIGH: 'mcp:tools:call:high',
} as const;

export const MCP_SCOPE_OPTIONS: EnumOption[] = [
  {label: MCP_SCOPES.TOOLS_LIST, value: MCP_SCOPES.TOOLS_LIST},
  {label: MCP_SCOPES.TOOLS_CALL, value: MCP_SCOPES.TOOLS_CALL},
  {label: MCP_SCOPES.TOOLS_CALL_HIGH, value: MCP_SCOPES.TOOLS_CALL_HIGH},
];

// Backend: MCP tool risk level
export const MCP_RISK_LEVELS = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
} as const;

export const MCP_RISK_LEVEL_OPTIONS: EnumOption[] = [
  {label: MCP_RISK_LEVELS.LOW, value: MCP_RISK_LEVELS.LOW},
  {label: MCP_RISK_LEVELS.MEDIUM, value: MCP_RISK_LEVELS.MEDIUM},
  {label: MCP_RISK_LEVELS.HIGH, value: MCP_RISK_LEVELS.HIGH},
];
