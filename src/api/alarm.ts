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

import { httpGet, httpPost } from '@/api/common';
import { API_DATA_BASE } from '@/config/constant/api';
import type { PageQuery, PageResult } from '@/config/types';
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

const add = <T>(base: string, payload: T) => httpPost(`${base}/add`, payload);
const update = <T>(base: string, payload: T) => httpPost(`${base}/update`, payload);
const remove = (base: string, id: string) => httpPost(`${base}/delete`, undefined, { params: { id } });
const selectById = <T>(base: string, id: string) => httpGet<R<T>>(`${base}/get_by_id`, { params: { id } });
const list = <T>(base: string, query: PageQuery) => httpPost<R<PageResult<T>>>(`${base}/list`, query);

export const addRule = (payload: Partial<RuleRecord>) => add(endpoints.rule, payload);
export const updateRule = (payload: Partial<RuleRecord>) => update(endpoints.rule, payload);
export const deleteRule = (id: string) => remove(endpoints.rule, id);
export const getRuleById = (id: string) => selectById<RuleRecord>(endpoints.rule, id);
export const listRule = (query: PageQuery) => list<RuleRecord>(endpoints.rule, query);

export const addNotify = (payload: Partial<NotifyRecord>) => add(endpoints.notify, payload);
export const updateNotify = (payload: Partial<NotifyRecord>) => update(endpoints.notify, payload);
export const deleteNotify = (id: string) => remove(endpoints.notify, id);
export const getNotifyById = (id: string) => selectById<NotifyRecord>(endpoints.notify, id);
export const listNotify = (query: PageQuery) => list<NotifyRecord>(endpoints.notify, query);

export const addMessage = (payload: Partial<MessageRecord>) => add(endpoints.message, payload);
export const updateMessage = (payload: Partial<MessageRecord>) => update(endpoints.message, payload);
export const deleteMessage = (id: string) => remove(endpoints.message, id);
export const getMessageById = (id: string) => selectById<MessageRecord>(endpoints.message, id);
export const listMessage = (query: PageQuery) => list<MessageRecord>(endpoints.message, query);

export const addNotifyChannel = (payload: Partial<NotifyChannelRecord>) => add(endpoints.channel, payload);
export const updateNotifyChannel = (payload: Partial<NotifyChannelRecord>) => update(endpoints.channel, payload);
export const deleteNotifyChannel = (id: string) => remove(endpoints.channel, id);
export const getNotifyChannelById = (id: string) => selectById<NotifyChannelRecord>(endpoints.channel, id);
export const listNotifyChannel = (query: PageQuery) => list<NotifyChannelRecord>(endpoints.channel, query);

export const addNotifyChannelBind = (payload: Partial<NotifyChannelBindRecord>) => add(endpoints.channelBind, payload);
export const updateNotifyChannelBind = (payload: Partial<NotifyChannelBindRecord>) =>
  update(endpoints.channelBind, payload);
export const deleteNotifyChannelBind = (id: string) => remove(endpoints.channelBind, id);
export const getNotifyChannelBindById = (id: string) => selectById<NotifyChannelBindRecord>(endpoints.channelBind, id);
export const listNotifyChannelBind = (query: PageQuery) => list<NotifyChannelBindRecord>(endpoints.channelBind, query);

export const getRuleStateById = (id: string) => selectById<RuleStateRecord>(endpoints.state, id);
export const listRuleState = (query: PageQuery) => list<RuleStateRecord>(endpoints.state, query);

export const getNotifyHistoryById = (id: string) => selectById<NotifyHistoryRecord>(endpoints.history, id);
export const listNotifyHistory = (query: PageQuery) => list<NotifyHistoryRecord>(endpoints.history, query);
