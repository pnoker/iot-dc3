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
import type {CommandHistoryRecord, CommandParamForm, CommandParamRecord, CommandRecord} from '@/config/types/command';

const endpoints = {
  command: `${API_MANAGER_BASE}/command`,
  commandParam: `${API_MANAGER_BASE}/command_param`,
  commandHistory: `${API_DATA_BASE}/command_history`,
} as const;

// Command Definition CRUD

export const addCommand = (payload: Partial<CommandRecord>) => crudAdd<Partial<CommandRecord>, CommandRecord>(endpoints.command, payload);
export const updateCommand = (payload: Partial<CommandRecord>) => crudUpdate<Partial<CommandRecord>, CommandRecord>(endpoints.command, payload);
export const deleteCommand = (id: string, version: number) => versionedDelete(endpoints.command, id, version);
export const getCommandById = (id: string) => crudGetById<CommandRecord>(endpoints.command, id);
export const listCommand = (query: PageQuery) => crudList<CommandRecord>(endpoints.command, query);
export const listCommandByProfileId = (profileId: string) =>
  httpGet<CommandRecord[]>(`${endpoints.command}/list_by_profile_id`, {params: {profile_id: profileId}});

// Command Param CRUD

export const addCommandParam = (payload: CommandParamForm) => crudAdd<CommandParamForm, CommandParamRecord>(endpoints.commandParam, payload);
export const updateCommandParam = (payload: Partial<CommandParamRecord>) =>
  crudUpdate<Partial<CommandParamRecord>, CommandParamRecord>(endpoints.commandParam, payload);
export const deleteCommandParam = (id: string, version: number) => versionedDelete(endpoints.commandParam, id, version);
export const listCommandParamByCommandId = (commandId: string) =>
  httpGet<CommandParamRecord[]>(`${endpoints.commandParam}/list_by_command_id`, {
    params: {command_id: commandId},
  });
export const listCommandParam = (query: PageQuery) =>
  httpPost<PageResult<CommandParamRecord>, PageQuery>(`${endpoints.commandParam}/list`, query);

// Command History Queries

export const getCommandHistoryByRecordId = (recordId: string) =>
  httpGet<CommandHistoryRecord>(`${endpoints.commandHistory}/get_by_record_id`, {params: {record_id: recordId}});
export const listCommandHistory = (query: PageQuery) =>
  httpPost<PageResult<CommandHistoryRecord>>(`${endpoints.commandHistory}/list`, query);
