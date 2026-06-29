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
import type {CommandHistoryRecord, CommandParamForm, CommandParamRecord, CommandRecord} from '@/config/types/command';

const endpoints = {
  command: `${API_MANAGER_BASE}/command`,
  commandParam: `${API_MANAGER_BASE}/command_param`,
  commandHistory: `${API_DATA_BASE}/command_history`,
} as const;

// Command Definition CRUD

export const addCommand = (payload: Partial<CommandRecord>) => crudAdd(endpoints.command, payload);
export const updateCommand = (payload: Partial<CommandRecord>) => crudUpdate(endpoints.command, payload);
export const deleteCommand = (id: string) => crudDelete(endpoints.command, id);
export const getCommandById = (id: string) => crudGetById<CommandRecord>(endpoints.command, id);
export const listCommand = (query: PageQuery) => crudList<CommandRecord>(endpoints.command, query);
export const listCommandByProfileId = (profileId: string) =>
  httpGet<R<CommandRecord[]>>(`${endpoints.command}/list_by_profile_id`, {params: {profile_id: profileId}});

// Command Param CRUD

export const addCommandParam = (payload: CommandParamForm) => crudAdd(endpoints.commandParam, payload);
export const updateCommandParam = (payload: Partial<CommandParamRecord>) => crudUpdate(endpoints.commandParam, payload);
export const deleteCommandParam = (id: string) => crudDelete(endpoints.commandParam, id);
export const listCommandParamByCommandId = (commandId: string) =>
  httpGet<R<CommandParamRecord[]>>(`${endpoints.commandParam}/list_by_command_id`, {
    params: {command_id: commandId},
  });

// Command History Queries

export const getCommandHistoryByRecordId = (recordId: string) =>
  httpGet<R<CommandHistoryRecord>>(`${endpoints.commandHistory}/get_by_record_id`, {params: {record_id: recordId}});
export const listCommandHistory = (query: PageQuery) =>
  httpPost<R<PageResult<CommandHistoryRecord>>>(`${endpoints.commandHistory}/list`, query);
