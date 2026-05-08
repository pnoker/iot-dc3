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

export const getDriverById = (id: string) => httpGet(`${API_MANAGER_BASE}/driver/id/${id}`);

export const getDriverByIds = (driverIds: any) => httpPost(`${API_MANAGER_BASE}/driver/ids`, driverIds);

export const getDriverList = <T = R>(driver: unknown) => httpPost<T>(`${API_MANAGER_BASE}/driver/list`, driver);

export const getDriverStatus = (driver: any) => httpPost(`${API_DATA_BASE}/driver/status/driver`, driver);
