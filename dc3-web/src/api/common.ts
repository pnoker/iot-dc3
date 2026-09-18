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

import request from '@/config/axios';
import type {AxiosRequestConfig} from 'axios';
import type {PageQuery, PageResult} from '@/config/types';

/**
 * Shared HTTP helpers so every `src/api/*.ts` module stays a one-liner per
 * endpoint. Keeps URL strings visible inline (grep-friendly) and lets the
 * response interceptor do the payload unwrapping.
 */

export const httpGet = <T = unknown>(url: string, config?: AxiosRequestConfig) => request<T>({...config, url, method: 'get'});

export const httpPost = <T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig) =>
  request<T>({...config, url, method: 'post', data});

export const httpPatch = <T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig) =>
  request<T>({...config, url, method: 'patch', data});

export const httpDelete = <T = unknown>(url: string, config?: AxiosRequestConfig) =>
  request<T>({...config, url, method: 'delete'});

export const crudAdd = <TPayload, TResponse = string>(base: string, payload: TPayload) =>
  httpPost<TResponse, TPayload>(`${base}/add`, payload);

export const crudUpdate = <TPayload, TResponse = string>(base: string, payload: TPayload) =>
  httpPost<TResponse, TPayload>(`${base}/update`, payload);

export const crudDelete = (base: string, id: string) =>
  httpDelete<void>(`${base}/delete`, {params: {id}});

export const versionedDelete = (base: string, id: string, version: number) =>
  httpDelete<void>(`${base}/delete`, {params: {id, version}});

export const crudGetById = <TRecord>(base: string, id: string) =>
  httpGet<TRecord>(`${base}/get_by_id`, {params: {id}});

export const crudList = <TRecord>(base: string, query: PageQuery) =>
  httpPost<PageResult<TRecord>, PageQuery>(`${base}/list`, query);
