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

import {crudAdd, crudDelete, crudGetById, crudList, crudUpdate, httpGet, httpPost} from '@/api/common';
import {API_DATA_BASE, API_MANAGER_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {EventHistoryRecord, EventParamForm, EventParamRecord, EventRecord} from '@/config/types/event';

const endpoints = {
  event: `${API_MANAGER_BASE}/event`,
  eventParam: `${API_MANAGER_BASE}/event_param`,
  eventHistory: `${API_DATA_BASE}/event_history`,
} as const;

// Event Definition CRUD

export const addEvent = (payload: Partial<EventRecord>) => crudAdd(endpoints.event, payload);
export const updateEvent = (payload: Partial<EventRecord>) => crudUpdate(endpoints.event, payload);
export const deleteEvent = (id: string) => crudDelete(endpoints.event, id);
export const getEventById = (id: string) => crudGetById<EventRecord>(endpoints.event, id);
export const listEvent = (query: PageQuery) => crudList<EventRecord>(endpoints.event, query);
export const listEventByProfileId = (profileId: string) =>
  httpGet<R<EventRecord[]>>(`${endpoints.event}/list_by_profile_id`, {params: {profile_id: profileId}});

// Event Param CRUD

export const addEventParam = (payload: EventParamForm) => crudAdd(endpoints.eventParam, payload);
export const updateEventParam = (payload: Partial<EventParamRecord>) => crudUpdate(endpoints.eventParam, payload);
export const deleteEventParam = (id: string) => crudDelete(endpoints.eventParam, id);
export const listEventParamByEventId = (eventId: string) =>
  httpGet<R<EventParamRecord[]>>(`${endpoints.eventParam}/list_by_event_id`, {params: {event_id: eventId}});

// Event History Queries

export const getEventHistoryByRecordId = (recordId: string) =>
  httpGet<R<EventHistoryRecord>>(`${endpoints.eventHistory}/get_by_record_id`, {params: {record_id: recordId}});
export const listEventHistory = (query: PageQuery) =>
  httpPost<R<PageResult<EventHistoryRecord>>>(`${endpoints.eventHistory}/list`, query);
