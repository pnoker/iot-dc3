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

import {createCrudApi} from '@/api/factory';
import {API_MANAGER_BASE} from '@/config/constant/api';
import type {GroupForm, GroupRecord} from '@/config/types/manager';

const crud = createCrudApi<GroupForm, GroupRecord>({base: API_MANAGER_BASE, entity: 'group'});

export const addGroup = crud.add;

export const deleteGroup = crud.delete;

export const updateGroup = crud.update;

export const getGroupById = crud.getById;

export const listGroup = crud.list;
