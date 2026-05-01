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

export const addMenu = (menu: any) => httpPost('api/v3/auth/menu/add', menu);

export const deleteMenu = (id: string) => httpPost(`api/v3/auth/menu/delete/${id}`);

export const updateMenu = (menu: any) => httpPost('api/v3/auth/menu/update', menu);

export const getMenuById = (id: string) => httpGet(`api/v3/auth/menu/id/${id}`);

export const getMenuList = (query: any) => httpPost('api/v3/auth/menu/list', query);

export const getMenuTree = (query: any = {}) => httpPost('api/v3/auth/menu/tree', query);
