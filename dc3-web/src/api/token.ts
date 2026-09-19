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

import {httpPost} from '@/api/common';
import {API_AUTH_BASE} from '@/config/constant/api';
import type {Login} from '@/config/types';

/**
 * Fetch the generate salt.
 * @param login - login payload with tenant, username and password
 * @returns the generated value
 */
export const generateSalt = (login: Login) => httpPost<string>(`${API_AUTH_BASE}/token/salt`, login);

/**
 * Fetch the generate token.
 * @param login - login payload with tenant, username and password
 * @returns the generated value
 */
export const generateToken = (login: Login) => httpPost(`${API_AUTH_BASE}/token/generate`, login);

/**
 * Change the password for the logged-in principal.
 * @param login - login payload with tenant, username and password
 * @returns the operation result
 */
export const changePassword = (login: Login) => httpPost(`${API_AUTH_BASE}/token/change_password`, login);

/**
 * Revoke the current token.
 * @param login - login payload with tenant, username and password
 * @returns the operation result
 */
export const cancelToken = (login: Login) => httpPost(`${API_AUTH_BASE}/token/cancel`, login);

/**
 * Fetch the check token valid.
 * @param login - login payload with tenant, username and password
 * @returns whether the token is still valid
 */
export const checkTokenValid = (login: Login) => httpPost(`${API_AUTH_BASE}/token/check`, login);
