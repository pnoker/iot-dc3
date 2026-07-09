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
