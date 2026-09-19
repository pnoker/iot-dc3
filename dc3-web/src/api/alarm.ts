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

import {crudAdd, crudGetById, crudList, crudUpdate, httpDelete} from '@/api/common';
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

/**
 * Create rule.
 * @param payload - request payload
 * @returns the operation result
 */
export const addRule = (payload: Partial<RuleRecord>) => crudAdd<Partial<RuleRecord>, RuleRecord>(endpoints.rule, payload);
/**
 * Update rule.
 * @param payload - request payload
 * @returns the operation result
 */
export const updateRule = (payload: Partial<RuleRecord>) => crudUpdate<Partial<RuleRecord>, RuleRecord>(endpoints.rule, payload);
/**
 * Delete rule.
 * @param id - record id
 * @returns the operation result
 */
export const deleteRule = (id: string) => httpDelete<void>(`${endpoints.rule}/delete`, {params: {id}});
/**
 * Fetch rule by id.
 * @param id - record id
 * @returns the fetched rule by id
 */
export const getRuleById = (id: string) => crudGetById<RuleRecord>(endpoints.rule, id);
/**
 * List rule.
 * @param query - page query with filters and paging
 * @returns the listed rule
 */
export const listRule = (query: PageQuery) => crudList<RuleRecord>(endpoints.rule, query);

/**
 * Create notify.
 * @param payload - request payload
 * @returns the operation result
 */
export const addNotify = (payload: Partial<NotifyRecord>) => crudAdd<Partial<NotifyRecord>, NotifyRecord>(endpoints.notify, payload);
/**
 * Update notify.
 * @param payload - request payload
 * @returns the operation result
 */
export const updateNotify = (payload: Partial<NotifyRecord>) => crudUpdate<Partial<NotifyRecord>, NotifyRecord>(endpoints.notify, payload);
/**
 * Delete notify.
 * @param id - record id
 * @returns the operation result
 */
export const deleteNotify = (id: string) => httpDelete<void>(`${endpoints.notify}/delete`, {params: {id}});
/**
 * Fetch notify by id.
 * @param id - record id
 * @returns the fetched notify by id
 */
export const getNotifyById = (id: string) => crudGetById<NotifyRecord>(endpoints.notify, id);
/**
 * List notify.
 * @param query - page query with filters and paging
 * @returns the listed notify
 */
export const listNotify = (query: PageQuery) => crudList<NotifyRecord>(endpoints.notify, query);

/**
 * Create message.
 * @param payload - request payload
 * @returns the operation result
 */
export const addMessage = (payload: Partial<MessageRecord>) => crudAdd<Partial<MessageRecord>, MessageRecord>(endpoints.message, payload);
/**
 * Update message.
 * @param payload - request payload
 * @returns the operation result
 */
export const updateMessage = (payload: Partial<MessageRecord>) => crudUpdate<Partial<MessageRecord>, MessageRecord>(endpoints.message, payload);
/**
 * Delete message.
 * @param id - record id
 * @returns the operation result
 */
export const deleteMessage = (id: string) => httpDelete<void>(`${endpoints.message}/delete`, {params: {id}});
/**
 * Fetch message by id.
 * @param id - record id
 * @returns the fetched message by id
 */
export const getMessageById = (id: string) => crudGetById<MessageRecord>(endpoints.message, id);
/**
 * List message.
 * @param query - page query with filters and paging
 * @returns the listed message
 */
export const listMessage = (query: PageQuery) => crudList<MessageRecord>(endpoints.message, query);

/**
 * Create notify channel.
 * @param payload - request payload
 * @returns the operation result
 */
export const addNotifyChannel = (payload: Partial<NotifyChannelRecord>) => crudAdd<Partial<NotifyChannelRecord>, NotifyChannelRecord>(endpoints.channel, payload);
/**
 * Update notify channel.
 * @param payload - request payload
 * @returns the operation result
 */
export const updateNotifyChannel = (payload: Partial<NotifyChannelRecord>) => crudUpdate<Partial<NotifyChannelRecord>, NotifyChannelRecord>(endpoints.channel, payload);
/**
 * Delete notify channel.
 * @param id - record id
 * @returns the operation result
 */
export const deleteNotifyChannel = (id: string) => httpDelete<void>(`${endpoints.channel}/delete`, {params: {id}});
/**
 * Fetch notify channel by id.
 * @param id - record id
 * @returns the fetched notify channel by id
 */
export const getNotifyChannelById = (id: string) => crudGetById<NotifyChannelRecord>(endpoints.channel, id);
/**
 * List notify channel.
 * @param query - page query with filters and paging
 * @returns the listed notify channel
 */
export const listNotifyChannel = (query: PageQuery) => crudList<NotifyChannelRecord>(endpoints.channel, query);

/**
 * Create notify channel bind.
 * @param payload - request payload
 * @returns the operation result
 */
export const addNotifyChannelBind = (payload: Partial<NotifyChannelBindRecord>) =>
  crudAdd<Partial<NotifyChannelBindRecord>, NotifyChannelBindRecord>(endpoints.channelBind, payload);
/**
 * Update notify channel bind.
 * @param payload - request payload
 * @returns the operation result
 */
export const updateNotifyChannelBind = (payload: Partial<NotifyChannelBindRecord>) =>
  crudUpdate<Partial<NotifyChannelBindRecord>, NotifyChannelBindRecord>(endpoints.channelBind, payload);
/**
 * Delete notify channel bind.
 * @param id - record id
 * @returns the operation result
 */
export const deleteNotifyChannelBind = (id: string) => httpDelete<void>(`${endpoints.channelBind}/delete`, {params: {id}});
/**
 * Fetch notify channel bind by id.
 * @param id - record id
 * @returns the fetched notify channel bind by id
 */
export const getNotifyChannelBindById = (id: string) => crudGetById<NotifyChannelBindRecord>(endpoints.channelBind, id);
/**
 * List notify channel bind.
 * @param query - page query with filters and paging
 * @returns the listed notify channel bind
 */
export const listNotifyChannelBind = (query: PageQuery) =>
  crudList<NotifyChannelBindRecord>(endpoints.channelBind, query);

/**
 * Fetch rule state by id.
 * @param id - record id
 * @returns the fetched rule state by id
 */
export const getRuleStateById = (id: string) => crudGetById<RuleStateRecord>(endpoints.state, id);
/**
 * List rule state.
 * @param query - page query with filters and paging
 * @returns the listed rule state
 */
export const listRuleState = (query: PageQuery) => crudList<RuleStateRecord>(endpoints.state, query);

/**
 * Fetch notify history by id.
 * @param id - record id
 * @returns the fetched notify history by id
 */
export const getNotifyHistoryById = (id: string) => crudGetById<NotifyHistoryRecord>(endpoints.history, id);
/**
 * List notify history.
 * @param query - page query with filters and paging
 * @returns the listed notify history
 */
export const listNotifyHistory = (query: PageQuery) => crudList<NotifyHistoryRecord>(endpoints.history, query);
