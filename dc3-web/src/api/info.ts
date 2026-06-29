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

import {httpGet, httpPost} from '@/api/common';
import {API_MANAGER_BASE} from '@/config/constant/api';
import type {CommandInfoForm, DriverInfoForm, EventInfoForm, PointInfoForm} from '@/config/types/manager';

export const addDriverInfo = (driverInfo: DriverInfoForm) =>
  httpPost(`${API_MANAGER_BASE}/driver_attribute_config/add`, driverInfo);

export const updateDriverInfo = (driverInfo: DriverInfoForm) =>
  httpPost(`${API_MANAGER_BASE}/driver_attribute_config/update`, driverInfo);

export const getDriverInfoByDeviceIdAndAttributeId = (deviceId: string, attributeId: string) =>
  httpGet(`${API_MANAGER_BASE}/driver_attribute_config/get_by_device_id_and_attribute_id`, {
    params: {device_id: deviceId, attribute_id: attributeId},
  });

export const listDriverInfoByDeviceId = (deviceId: string) =>
  httpGet(`${API_MANAGER_BASE}/driver_attribute_config/list_by_device_id`, {params: {device_id: deviceId}});

export const addPointInfo = (pointInfo: PointInfoForm) =>
  httpPost(`${API_MANAGER_BASE}/point_attribute_config/add`, pointInfo);

export const updatePointInfo = (pointInfo: PointInfoForm) =>
  httpPost(`${API_MANAGER_BASE}/point_attribute_config/update`, pointInfo);

export const listPointInfoByDeviceIdAndPointId = (deviceId: string, pointId: string) =>
  httpGet(`${API_MANAGER_BASE}/point_attribute_config/list_by_device_id_and_point_id`, {
    params: {device_id: deviceId, point_id: pointId},
  });

export const listPointInfoByDeviceId = (deviceId: string) =>
  httpGet(`${API_MANAGER_BASE}/point_attribute_config/list_by_device_id`, {params: {device_id: deviceId}});

export const addCommandInfo = (commandInfo: CommandInfoForm) =>
  httpPost(`${API_MANAGER_BASE}/command_attribute_config/add`, commandInfo);

export const updateCommandInfo = (commandInfo: CommandInfoForm) =>
  httpPost(`${API_MANAGER_BASE}/command_attribute_config/update`, commandInfo);

export const listCommandInfoByDeviceIdAndCommandId = (deviceId: string, commandId: string) =>
  httpGet(`${API_MANAGER_BASE}/command_attribute_config/list_by_device_id_and_command_id`, {
    params: {device_id: deviceId, command_id: commandId},
  });

export const listCommandInfoByDeviceId = (deviceId: string) =>
  httpGet(`${API_MANAGER_BASE}/command_attribute_config/list_by_device_id`, {params: {device_id: deviceId}});

export const addEventInfo = (eventInfo: EventInfoForm) =>
  httpPost(`${API_MANAGER_BASE}/event_attribute_config/add`, eventInfo);

export const updateEventInfo = (eventInfo: EventInfoForm) =>
  httpPost(`${API_MANAGER_BASE}/event_attribute_config/update`, eventInfo);

export const listEventInfoByDeviceIdAndEventId = (deviceId: string, eventId: string) =>
  httpGet(`${API_MANAGER_BASE}/event_attribute_config/list_by_device_id_and_event_id`, {
    params: {device_id: deviceId, event_id: eventId},
  });

export const listEventInfoByDeviceId = (deviceId: string) =>
  httpGet(`${API_MANAGER_BASE}/event_attribute_config/list_by_device_id`, {params: {device_id: deviceId}});
