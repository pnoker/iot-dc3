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

/**
 * Perform a GET request and unwrap the response payload.
 * @param url - request url
 * @param config - configuration object
 * @returns the response payload
 */
export const httpGet = <T = unknown>(url: string, config?: AxiosRequestConfig) => request<T>({...config, url, method: 'get'});

/**
 * Perform a POST request and unwrap the response payload.
 * @param url - request url
 * @param data - payload to send or render
 * @param config - configuration object
 * @returns the response payload
 */
export const httpPost = <T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig) =>
  request<T>({...config, url, method: 'post', data});

/**
 * Perform a PATCH request and unwrap the response payload.
 * @param url - request url
 * @param data - payload to send or render
 * @param config - configuration object
 * @returns the response payload
 */
export const httpPatch = <T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig) =>
  request<T>({...config, url, method: 'patch', data});

/**
 * Perform a DELETE request and unwrap the response payload.
 * @param url - request url
 * @param config - configuration object
 * @returns the response payload
 */
export const httpDelete = <T = unknown>(url: string, config?: AxiosRequestConfig) =>
  request<T>({...config, url, method: 'delete'});

/**
 * Create a record via the standard `{base}/add` endpoint.
 * @param base - API base path of the resource collection
 * @param payload - request payload
 * @returns the created record id
 */
export const crudAdd = <TPayload, TResponse = string>(base: string, payload: TPayload) =>
  httpPost<TResponse, TPayload>(`${base}/add`, payload);

/**
 * Update a record via the standard `{base}/update` endpoint.
 * @param base - API base path of the resource collection
 * @param payload - request payload
 * @returns the updated record
 */
export const crudUpdate = <TPayload, TResponse = string>(base: string, payload: TPayload) =>
  httpPost<TResponse, TPayload>(`${base}/update`, payload);

/**
 * Delete a record via the standard `{base}/delete` endpoint.
 * @param base - API base path of the resource collection
 * @param id - record id
 * @returns the deletion result
 */
export const crudDelete = (base: string, id: string) =>
  httpDelete<void>(`${base}/delete`, {params: {id}});

/**
 * Delete a record with an optimistic-lock version check.
 * @param base - API base path of the resource collection
 * @param id - record id
 * @param version - record version for optimistic locking
 * @returns the deletion request promise
 */
export const versionedDelete = (base: string, id: string, version: number) =>
  httpDelete<void>(`${base}/delete`, {params: {id, version}});

/**
 * Fetch a single record via the standard `{base}/get_by_id` endpoint.
 * @param base - API base path of the resource collection
 * @param id - record id
 * @returns the fetched record
 */
export const crudGetById = <TRecord>(base: string, id: string) =>
  httpGet<TRecord>(`${base}/get_by_id`, {params: {id}});

/**
 * List records with paging via the standard `{base}/list` endpoint.
 * @param base - API base path of the resource collection
 * @param query - page query with filters and paging
 * @returns the paged result
 */
export const crudList = <TRecord>(base: string, query: PageQuery) =>
  httpPost<PageResult<TRecord>, PageQuery>(`${base}/list`, query);
