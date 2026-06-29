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

import {httpPost} from '@/api/common';
import {API_AUTH_BASE} from '@/config/constant/api';
import type {Login} from '@/config/types';

export const generateSalt = (login: Login) => httpPost(`${API_AUTH_BASE}/token/salt`, login);

export const generateToken = (login: Login) => httpPost(`${API_AUTH_BASE}/token/generate`, login);

export const changePassword = (login: Login) => httpPost(`${API_AUTH_BASE}/token/change_password`, login);

export const cancelToken = (login: Login) => httpPost(`${API_AUTH_BASE}/token/cancel`, login);

export const checkTokenValid = (login: Login) => httpPost(`${API_AUTH_BASE}/token/check`, login);
