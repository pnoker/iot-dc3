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
import { API_DATA_BASE, API_MANAGER_BASE } from '@/config/constant/api';
import type { PageQuery, PageResult } from '@/config/types';
import type { EventHistory, EventRecord } from '@/config/types/event';

const endpoints = {
  event: `${API_MANAGER_BASE}/event`,
  eventHistory: `${API_DATA_BASE}/event_history`,
} as const;

const add = <T>(base: string, payload: T) => httpPost(`${base}/add`, payload);
const update = <T>(base: string, payload: T) => httpPost(`${base}/update`, payload);
const remove = (base: string, id: string) => httpPost(`${base}/delete`, undefined, { params: { id } });
const selectById = <T>(base: string, id: string) => httpGet<R<T>>(`${base}/get_by_id`, { params: { id } });
const list = <T>(base: string, query: PageQuery) => httpPost<R<PageResult<T>>>(`${base}/list`, query);

// ─── Event Definition CRUD ────────────────────────────────────────────

export const addEvent = (payload: Partial<EventRecord>) => add(endpoints.event, payload);
export const updateEvent = (payload: Partial<EventRecord>) => update(endpoints.event, payload);
export const deleteEvent = (id: string) => remove(endpoints.event, id);
export const getEventById = (id: string) => selectById<EventRecord>(endpoints.event, id);
export const listEvent = (query: PageQuery) => list<EventRecord>(endpoints.event, query);
export const listEventByProfileId = (profileId: string) =>
  httpGet<R<EventRecord[]>>(`${endpoints.event}/list_by_profile_id`, { params: { profile_id: profileId } });

// ─── Event History Queries ────────────────────────────────────────────

export const getEventHistoryById = (recordId: string) =>
  httpGet<R<EventHistory>>(`${endpoints.eventHistory}/${recordId}`);
export const listEventHistory = (query: PageQuery) =>
  httpPost<R<PageResult<EventHistory>>>(`${endpoints.eventHistory}/list`, query);
