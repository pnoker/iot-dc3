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
