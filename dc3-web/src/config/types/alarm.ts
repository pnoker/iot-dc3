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

export interface AlarmBase {
  id: string;
  remark?: string;
  createTime?: string;
  operateTime?: string;
  creatorName?: string;
  operatorName?: string;

  [key: string]: unknown;
}

export interface RuleRecord extends AlarmBase {
  alarmTargetTypeFlag?: AlarmTargetTypeFlag;
  ruleName?: string;
  ruleCode?: string;
  entityId?: string;
  notifyId?: string;
  messageId?: string;
  ruleExt?: StructuredExt;
  enableFlag?: EnableFlag;
}

export interface NotifyRecord extends AlarmBase {
  notifyName?: string;
  notifyCode?: string;
  autoConfirmFlag?: AutoConfirmFlag;
  notifyInterval?: number | string;
  notifyExt?: StructuredExt;
  enableFlag?: EnableFlag;
}

export interface MessageRecord extends AlarmBase {
  messageName?: string;
  messageCode?: string;
  messageLevel?: number | string;
  messageExt?: StructuredExt;
  enableFlag?: EnableFlag;
}

export interface NotifyChannelRecord extends AlarmBase {
  channelName?: string;
  channelCode?: string;
  channelTypeFlag?: NotifyChannelTypeFlag;
  credentialRef?: string;
  channelExt?: StructuredExt;
  enableFlag?: EnableFlag;
}

export interface NotifyChannelBindRecord extends AlarmBase {
  notifyId?: string;
  channelId?: string;
  bindExt?: StructuredExt;
  enableFlag?: EnableFlag;
}

export interface RuleStateRecord extends AlarmBase {
  ruleId?: string;
  alarmTargetTypeFlag?: AlarmTargetTypeFlag;
  entityId?: string;
  fingerprint?: string;
  entityStateFlag?: RuleStateFlag;
  firstTriggerTime?: string;
  lastTriggerTime?: string;
  lastRecoverTime?: string;
  lastNotifyTime?: string;
  triggerCount?: number | string;
  alarmId?: string;
  entityStateExt?: StructuredExt;
}

export interface NotifyHistoryRecord extends AlarmBase {
  ruleId?: string;
  notifyId?: string;
  messageId?: string;
  channelId?: string;
  alarmId?: string;
  channelTypeFlag?: NotifyChannelTypeFlag;
  target?: string;
  statusFlag?: NotifyHistoryStatusFlag;
  requestExt?: StructuredExt;
  responseExt?: StructuredExt;
  errorMessage?: string;
  retryCount?: number | string;
}

export type AlarmEntity =
  RuleRecord | NotifyRecord | MessageRecord | NotifyChannelBindRecord | RuleStateRecord | NotifyHistoryRecord;
