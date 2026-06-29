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

import request from '@/config/axios';
import type {AxiosRequestConfig} from 'axios';
import type {PageQuery, PageResult} from '@/config/types';

/**
 * Shared HTTP helpers so every `src/api/*.ts` module stays a one-liner per
 * endpoint. Keeps URL strings visible inline (grep-friendly) and lets the
 * response interceptor do the payload unwrapping.
 */

export const httpGet = <T = R>(url: string, config?: AxiosRequestConfig) => request<T>({...config, url, method: 'get'});

export const httpPost = <T = R, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig) =>
  request<T>({...config, url, method: 'post', data});

export const crudAdd = <TPayload, TResponse = string>(base: string, payload: TPayload) =>
  httpPost<R<TResponse>, TPayload>(`${base}/add`, payload);

export const crudUpdate = <TPayload, TResponse = string>(base: string, payload: TPayload) =>
  httpPost<R<TResponse>, TPayload>(`${base}/update`, payload);

export const crudDelete = (base: string, id: string) =>
  httpPost<R<string>>(`${base}/delete`, undefined, {params: {id}});

export const crudGetById = <TRecord>(base: string, id: string) =>
  httpGet<R<TRecord>>(`${base}/get_by_id`, {params: {id}});

export const crudList = <TRecord>(base: string, query: PageQuery) =>
  httpPost<R<PageResult<TRecord>>, PageQuery>(`${base}/list`, query);
