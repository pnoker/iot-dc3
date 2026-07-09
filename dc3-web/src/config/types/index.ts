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

export type {Login, Attribute, Dictionary, Order, PageQuery, PageResult} from './common';

export type {
  AgenticModel,
  AgenticModelConfig,
  AgenticProvider,
  AgenticSession,
  AgenticSessionExt,
  AgenticMessage,
  AgenticMessageContext,
  AgenticMessageTokens,
  AgenticMessageRole,
  AgenticVisualizationAnnotation,
  AgenticVisualizationSpec,
  AgenticVisualizationType,
  AgenticAttachment,
  AgenticAction,
  AgenticChatMessage,
  AgenticChatCompletionRequest,
  AgenticChatCompletionResponse,
  AgenticStreamCallbacks,
  AgenticTraceEvent,
} from './agentic';

export type {
  AlarmTargetTypeFlag,
  NotifyChannelTypeFlag,
  RuleStateFlag,
  NotifyHistoryStatusFlag,
  AutoConfirmFlag,
  EnableFlag,
  StructuredExt,
  RuleRecord,
  NotifyRecord,
  MessageRecord,
  NotifyChannelRecord,
  NotifyChannelBindRecord,
  RuleStateRecord,
  NotifyHistoryRecord,
  AlarmEntity,
} from './alarm';

export type {CommandRecord, CommandForm, CommandParamRecord, CommandParamForm, CommandHistoryRecord} from './command';
export type {EventRecord, EventForm, EventParamRecord, EventParamForm, EventHistoryRecord} from './event';

export type {
  UserForm,
  UserRecord,
  RoleForm,
  RoleRecord,
  MenuForm,
  MenuRecord,
  ResourceForm,
  ResourceRecord,
  ApiForm,
  ApiRecord,
  RolePrincipalBindForm,
  RolePrincipalBindRecord,
  RoleResourceBindForm,
  ServiceAccountForm,
  ServiceAccountRecord,
  PrincipalForm,
  PrincipalRecord,
  TenantMembershipForm,
  TenantMembershipRecord,
  LocalCredentialForm,
  LocalCredentialRecord,
  McpClientRegistrationForm,
  OAuthClientRecord,
  McpConnectionForm,
  McpConnectionRecord,
  McpToolRecord,
  McpAuditRecord,
  IdentityAuditRecord,
} from './auth';

export type {
  DeviceForm,
  DeviceRecord,
  DriverRecord,
  ProfileForm,
  ProfileRecord,
  PointForm,
  PointRecord,
  CommandInfoForm,
  DriverInfoForm,
  EventInfoForm,
  PointInfoForm,
} from './manager';
