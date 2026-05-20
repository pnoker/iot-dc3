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
 * Alarm / notification data-domain types.
 */

export type AlarmTargetTypeFlag = 'POINT' | 'DEVICE' | 'DRIVER' | string;
export type NotifyChannelTypeFlag = 'FEISHU_BOT' | 'WEBHOOK' | 'EMAIL' | string;
export type RuleStateFlag = 'NORMAL' | 'FIRING' | 'RECOVERED' | string;
export type NotifyHistoryStatusFlag = 'PENDING' | 'SUCCESS' | 'FAILED' | 'RETRYING' | 'SKIPPED' | string;
export type AutoConfirmFlag = 'AUTO' | 'MANUAL' | string;
export type EnableFlag = 'ENABLE' | 'DISABLE' | string;

export interface StructuredExt<T = Record<string, unknown>> {
  type?: string;
  version?: number;
  remark?: string;
  content?: T;
}

export interface AlarmBaseRecord {
  id: string;
  remark?: string;
  createTime?: string;
  operateTime?: string;
  creatorName?: string;
  operatorName?: string;
  [key: string]: unknown;
}

export interface RuleRecord extends AlarmBaseRecord {
  alarmTargetTypeFlag?: AlarmTargetTypeFlag;
  ruleName?: string;
  ruleCode?: string;
  entityId?: string;
  notifyId?: string;
  messageId?: string;
  ruleExt?: StructuredExt;
  enableFlag?: EnableFlag;
}

export interface NotifyRecord extends AlarmBaseRecord {
  notifyName?: string;
  notifyCode?: string;
  autoConfirmFlag?: AutoConfirmFlag;
  notifyInterval?: number | string;
  notifyExt?: StructuredExt;
  enableFlag?: EnableFlag;
}

export interface MessageRecord extends AlarmBaseRecord {
  messageName?: string;
  messageCode?: string;
  messageLevel?: number | string;
  messageExt?: StructuredExt;
  enableFlag?: EnableFlag;
}

export interface NotifyChannelRecord extends AlarmBaseRecord {
  channelName?: string;
  channelCode?: string;
  channelTypeFlag?: NotifyChannelTypeFlag;
  credentialRef?: string;
  channelExt?: StructuredExt;
  enableFlag?: EnableFlag;
}

export interface NotifyChannelBindRecord extends AlarmBaseRecord {
  notifyId?: string;
  channelId?: string;
  bindExt?: StructuredExt;
  enableFlag?: EnableFlag;
}

export interface RuleStateRecord extends AlarmBaseRecord {
  ruleId?: string;
  alarmTargetTypeFlag?: AlarmTargetTypeFlag;
  entityId?: string;
  fingerprint?: string;
  stateFlag?: RuleStateFlag;
  firstTriggerTime?: string;
  lastTriggerTime?: string;
  lastRecoverTime?: string;
  lastNotifyTime?: string;
  triggerCount?: number | string;
  eventId?: string;
  stateExt?: StructuredExt;
}

export interface NotifyHistoryRecord extends AlarmBaseRecord {
  ruleId?: string;
  notifyId?: string;
  messageId?: string;
  channelId?: string;
  eventId?: string;
  channelTypeFlag?: NotifyChannelTypeFlag;
  target?: string;
  statusFlag?: NotifyHistoryStatusFlag;
  requestExt?: StructuredExt;
  responseExt?: StructuredExt;
  errorMessage?: string;
  retryCount?: number | string;
}

export type AlarmEntityRecord =
  | RuleRecord
  | NotifyRecord
  | MessageRecord
  | NotifyChannelRecord
  | NotifyChannelBindRecord
  | RuleStateRecord
  | NotifyHistoryRecord;
