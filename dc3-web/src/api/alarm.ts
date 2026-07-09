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

import {crudAdd, crudDelete, crudGetById, crudList, crudUpdate} from '@/api/common';
import {API_DATA_BASE} from '@/config/constant/api';
import type {PageQuery} from '@/config/types';
import type {
  MessageRecord,
  NotifyChannelBindRecord,
  NotifyChannelRecord,
  NotifyHistoryRecord,
  NotifyRecord,
  RuleRecord,
  RuleStateRecord,
} from '@/config/types/alarm';

const endpoints = {
  rule: `${API_DATA_BASE}/rule`,
  notify: `${API_DATA_BASE}/notify`,
  message: `${API_DATA_BASE}/message`,
  channel: `${API_DATA_BASE}/notify/channel`,
  channelBind: `${API_DATA_BASE}/notify/channel/bind`,
  state: `${API_DATA_BASE}/rule/state`,
  history: `${API_DATA_BASE}/notify/history`,
} as const;

export const addRule = (payload: Partial<RuleRecord>) => crudAdd(endpoints.rule, payload);
export const updateRule = (payload: Partial<RuleRecord>) => crudUpdate(endpoints.rule, payload);
export const deleteRule = (id: string) => crudDelete(endpoints.rule, id);
export const getRuleById = (id: string) => crudGetById<RuleRecord>(endpoints.rule, id);
export const listRule = (query: PageQuery) => crudList<RuleRecord>(endpoints.rule, query);

export const addNotify = (payload: Partial<NotifyRecord>) => crudAdd(endpoints.notify, payload);
export const updateNotify = (payload: Partial<NotifyRecord>) => crudUpdate(endpoints.notify, payload);
export const deleteNotify = (id: string) => crudDelete(endpoints.notify, id);
export const getNotifyById = (id: string) => crudGetById<NotifyRecord>(endpoints.notify, id);
export const listNotify = (query: PageQuery) => crudList<NotifyRecord>(endpoints.notify, query);

export const addMessage = (payload: Partial<MessageRecord>) => crudAdd(endpoints.message, payload);
export const updateMessage = (payload: Partial<MessageRecord>) => crudUpdate(endpoints.message, payload);
export const deleteMessage = (id: string) => crudDelete(endpoints.message, id);
export const getMessageById = (id: string) => crudGetById<MessageRecord>(endpoints.message, id);
export const listMessage = (query: PageQuery) => crudList<MessageRecord>(endpoints.message, query);

export const addNotifyChannel = (payload: Partial<NotifyChannelRecord>) => crudAdd(endpoints.channel, payload);
export const updateNotifyChannel = (payload: Partial<NotifyChannelRecord>) => crudUpdate(endpoints.channel, payload);
export const deleteNotifyChannel = (id: string) => crudDelete(endpoints.channel, id);
export const getNotifyChannelById = (id: string) => crudGetById<NotifyChannelRecord>(endpoints.channel, id);
export const listNotifyChannel = (query: PageQuery) => crudList<NotifyChannelRecord>(endpoints.channel, query);

export const addNotifyChannelBind = (payload: Partial<NotifyChannelBindRecord>) =>
  crudAdd(endpoints.channelBind, payload);
export const updateNotifyChannelBind = (payload: Partial<NotifyChannelBindRecord>) =>
  crudUpdate(endpoints.channelBind, payload);
export const deleteNotifyChannelBind = (id: string) => crudDelete(endpoints.channelBind, id);
export const getNotifyChannelBindById = (id: string) => crudGetById<NotifyChannelBindRecord>(endpoints.channelBind, id);
export const listNotifyChannelBind = (query: PageQuery) =>
  crudList<NotifyChannelBindRecord>(endpoints.channelBind, query);

export const getRuleStateById = (id: string) => crudGetById<RuleStateRecord>(endpoints.state, id);
export const listRuleState = (query: PageQuery) => crudList<RuleStateRecord>(endpoints.state, query);

export const getNotifyHistoryById = (id: string) => crudGetById<NotifyHistoryRecord>(endpoints.history, id);
export const listNotifyHistory = (query: PageQuery) => crudList<NotifyHistoryRecord>(endpoints.history, query);
