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
