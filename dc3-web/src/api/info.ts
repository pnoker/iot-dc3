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

import {httpGet, httpPatch, httpPost, versionedDelete} from '@/api/common';
import {API_MANAGER_BASE} from '@/config/constant/api';
import type {CommandInfoForm, DriverInfoForm, EventInfoForm, PointInfoForm} from '@/config/types/manager';

export const addDriverInfo = (driverInfo: DriverInfoForm) =>
  httpPost<DriverInfoForm>(`${API_MANAGER_BASE}/driver_attribute_config/add`, driverInfo);

export const updateDriverInfo = (driverInfo: DriverInfoForm) =>
  httpPatch<DriverInfoForm>(`${API_MANAGER_BASE}/driver_attribute_config/update`, driverInfo);

export const deleteDriverInfo = (id: string, version: number) => versionedDelete(`${API_MANAGER_BASE}/driver_attribute_config`, id, version);

export const getDriverInfoByDeviceIdAndAttributeId = (deviceId: string, attributeId: string) =>
  httpGet<DriverInfoForm>(`${API_MANAGER_BASE}/driver_attribute_config/get_by_device_id_and_attribute_id`, {
    params: {device_id: deviceId, attribute_id: attributeId},
  });

export const listDriverInfoByDeviceId = (deviceId: string) =>
  httpGet<DriverInfoForm[]>(`${API_MANAGER_BASE}/driver_attribute_config/list_by_device_id`, {params: {device_id: deviceId}});

export const addPointInfo = (pointInfo: PointInfoForm) =>
  httpPost<PointInfoForm>(`${API_MANAGER_BASE}/point_attribute_config/add`, pointInfo);

export const updatePointInfo = (pointInfo: PointInfoForm) =>
  httpPatch<PointInfoForm>(`${API_MANAGER_BASE}/point_attribute_config/update`, pointInfo);

export const deletePointInfo = (id: string, version: number) => versionedDelete(`${API_MANAGER_BASE}/point_attribute_config`, id, version);

export const listPointInfoByDeviceIdAndPointId = (deviceId: string, pointId: string) =>
  httpGet<PointInfoForm[]>(`${API_MANAGER_BASE}/point_attribute_config/list_by_device_id_and_point_id`, {
    params: {device_id: deviceId, point_id: pointId},
  });

export const listPointInfoByDeviceId = (deviceId: string) =>
  httpGet<PointInfoForm[]>(`${API_MANAGER_BASE}/point_attribute_config/list_by_device_id`, {params: {device_id: deviceId}});

export const addCommandInfo = (commandInfo: CommandInfoForm) =>
  httpPost<CommandInfoForm>(`${API_MANAGER_BASE}/command_attribute_config/add`, commandInfo);

export const updateCommandInfo = (commandInfo: CommandInfoForm) =>
  httpPatch<CommandInfoForm>(`${API_MANAGER_BASE}/command_attribute_config/update`, commandInfo);

export const deleteCommandInfo = (id: string, version: number) => versionedDelete(`${API_MANAGER_BASE}/command_attribute_config`, id, version);

export const listCommandInfoByDeviceIdAndCommandId = (deviceId: string, commandId: string) =>
  httpGet<CommandInfoForm[]>(`${API_MANAGER_BASE}/command_attribute_config/list_by_device_id_and_command_id`, {
    params: {device_id: deviceId, command_id: commandId},
  });

export const listCommandInfoByDeviceId = (deviceId: string) =>
  httpGet<CommandInfoForm[]>(`${API_MANAGER_BASE}/command_attribute_config/list_by_device_id`, {params: {device_id: deviceId}});

export const addEventInfo = (eventInfo: EventInfoForm) =>
  httpPost<EventInfoForm>(`${API_MANAGER_BASE}/event_attribute_config/add`, eventInfo);

export const updateEventInfo = (eventInfo: EventInfoForm) =>
  httpPatch<EventInfoForm>(`${API_MANAGER_BASE}/event_attribute_config/update`, eventInfo);

export const deleteEventInfo = (id: string, version: number) => versionedDelete(`${API_MANAGER_BASE}/event_attribute_config`, id, version);

export const listEventInfoByDeviceIdAndEventId = (deviceId: string, eventId: string) =>
  httpGet<EventInfoForm[]>(`${API_MANAGER_BASE}/event_attribute_config/list_by_device_id_and_event_id`, {
    params: {device_id: deviceId, event_id: eventId},
  });

export const listEventInfoByDeviceId = (deviceId: string) =>
  httpGet<EventInfoForm[]>(`${API_MANAGER_BASE}/event_attribute_config/list_by_device_id`, {params: {device_id: deviceId}});
