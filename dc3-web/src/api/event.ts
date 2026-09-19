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

import {crudAdd, crudGetById, crudList, crudUpdate, httpGet, httpPost, versionedDelete} from '@/api/common';
import {API_DATA_BASE, API_MANAGER_BASE} from '@/config/constant/api';
import type {PageQuery, PageResult} from '@/config/types';
import type {EventHistoryRecord, EventParamForm, EventParamRecord, EventRecord} from '@/config/types/event';

const endpoints = {
  event: `${API_MANAGER_BASE}/event`,
  eventParam: `${API_MANAGER_BASE}/event_param`,
  eventHistory: `${API_DATA_BASE}/event_history`,
} as const;

// Event Definition CRUD

/**
 * Create event.
 * @param payload - request payload
 * @returns the operation result
 */
export const addEvent = (payload: Partial<EventRecord>) => crudAdd<Partial<EventRecord>, EventRecord>(endpoints.event, payload);
/**
 * Update event.
 * @param payload - request payload
 * @returns the operation result
 */
export const updateEvent = (payload: Partial<EventRecord>) => crudUpdate<Partial<EventRecord>, EventRecord>(endpoints.event, payload);
/**
 * Delete event.
 * @param id - record id
 * @param version - record version for optimistic locking
 * @returns the operation result
 */
export const deleteEvent = (id: string, version: number) => versionedDelete(endpoints.event, id, version);
/**
 * Fetch event by id.
 * @param id - record id
 * @returns the fetched event by id
 */
export const getEventById = (id: string) => crudGetById<EventRecord>(endpoints.event, id);
/**
 * List event.
 * @param query - page query with filters and paging
 * @returns the listed event
 */
export const listEvent = (query: PageQuery) => crudList<EventRecord>(endpoints.event, query);
/**
 * List event by profile id.
 * @param profileId - profile id to scope the request
 * @returns the listed event by profile id
 */
export const listEventByProfileId = (profileId: string) =>
  httpGet<EventRecord[]>(`${endpoints.event}/list_by_profile_id`, {params: {profile_id: profileId}});

// Event Param CRUD

/**
 * Create event param.
 * @param payload - request payload
 * @returns the operation result
 */
export const addEventParam = (payload: EventParamForm) => crudAdd<EventParamForm, EventParamRecord>(endpoints.eventParam, payload);
/**
 * Update event param.
 * @param payload - request payload
 * @returns the operation result
 */
export const updateEventParam = (payload: Partial<EventParamRecord>) =>
  crudUpdate<Partial<EventParamRecord>, EventParamRecord>(endpoints.eventParam, payload);
/**
 * Delete event param.
 * @param id - record id
 * @param version - record version for optimistic locking
 * @returns the operation result
 */
export const deleteEventParam = (id: string, version: number) => versionedDelete(endpoints.eventParam, id, version);
/**
 * List event param by event id.
 * @param eventId - event id to scope the request
 * @returns the listed event param by event id
 */
export const listEventParamByEventId = (eventId: string) =>
  httpGet<EventParamRecord[]>(`${endpoints.eventParam}/list_by_event_id`, {params: {event_id: eventId}});
/**
 * List event param.
 * @param query - page query with filters and paging
 * @returns the listed event param
 */
export const listEventParam = (query: PageQuery) =>
  httpPost<PageResult<EventParamRecord>, PageQuery>(`${endpoints.eventParam}/list`, query);

// Event History Queries

/**
 * Fetch event history by record id.
 * @param recordId - record id to scope the request
 * @returns the fetched event history by record id
 */
export const getEventHistoryByRecordId = (recordId: string) =>
  httpGet<EventHistoryRecord>(`${endpoints.eventHistory}/get_by_record_id`, {params: {record_id: recordId}});
/**
 * List event history.
 * @param query - page query with filters and paging
 * @returns the listed event history
 */
export const listEventHistory = (query: PageQuery) =>
  httpPost<PageResult<EventHistoryRecord>>(`${endpoints.eventHistory}/list`, query);
