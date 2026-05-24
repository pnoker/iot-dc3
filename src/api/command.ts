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
import type { CommandHistory, CommandRecord } from '@/config/types/command';

const endpoints = {
  command: `${API_MANAGER_BASE}/command`,
  commandHistory: `${API_DATA_BASE}/command_history`,
} as const;

const add = <T>(base: string, payload: T) => httpPost(`${base}/add`, payload);
const update = <T>(base: string, payload: T) => httpPost(`${base}/update`, payload);
const remove = (base: string, id: string) => httpPost(`${base}/delete`, undefined, { params: { id } });
const selectById = <T>(base: string, id: string) => httpGet<R<T>>(`${base}/get_by_id`, { params: { id } });
const list = <T>(base: string, query: PageQuery) => httpPost<R<PageResult<T>>>(`${base}/list`, query);

// ─── Command Definition CRUD ─────────────────────────────────────────

export const addCommand = (payload: Partial<CommandRecord>) => add(endpoints.command, payload);
export const updateCommand = (payload: Partial<CommandRecord>) => update(endpoints.command, payload);
export const deleteCommand = (id: string) => remove(endpoints.command, id);
export const getCommandById = (id: string) => selectById<CommandRecord>(endpoints.command, id);
export const listCommand = (query: PageQuery) => list<CommandRecord>(endpoints.command, query);
export const listCommandByProfileId = (profileId: string) =>
  httpGet<R<CommandRecord[]>>(`${endpoints.command}/list_by_profile_id`, { params: { profile_id: profileId } });

// ─── Command History Queries ──────────────────────────────────────────

export const getCommandHistoryById = (recordId: string) =>
  httpGet<R<CommandHistory>>(`${endpoints.commandHistory}/${recordId}`);
export const listCommandHistory = (query: PageQuery) =>
  httpPost<R<PageResult<CommandHistory>>>(`${endpoints.commandHistory}/list`, query);
