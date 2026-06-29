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

import {httpGet} from '@/api/common';
import {API_MANAGER_BASE} from '@/config/constant/api';
import type {Attribute} from '@/config/types';

export const listDriverAttributeByDriverId = (id: string) =>
  httpGet<R<Attribute[]>>(`${API_MANAGER_BASE}/driver_attribute/list_by_driver_id`, {params: {driver_id: id}});

export const listPointAttributeByDriverId = (id: string) =>
  httpGet<R<Attribute[]>>(`${API_MANAGER_BASE}/point_attribute/list_by_driver_id`, {params: {driver_id: id}});

export const listCommandAttributeByDriverId = (id: string) =>
  httpGet<R<Attribute[]>>(`${API_MANAGER_BASE}/command_attribute/list_by_driver_id`, {params: {driver_id: id}});

export const listEventAttributeByDriverId = (id: string) =>
  httpGet<R<Attribute[]>>(`${API_MANAGER_BASE}/event_attribute/list_by_driver_id`, {params: {driver_id: id}});
