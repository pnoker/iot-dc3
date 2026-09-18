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

import {httpDelete, httpGet, httpPost} from '@/api/common';
import type {PageQuery, PageResult} from '@/config/types';

/**
 * The five canonical CRUD endpoints every pure resource module wires up by hand:
 * add / delete / update / get_by_id / list. `createCrudApi` bundles them behind
 * one prefix so a module file shrinks to its non-standard endpoints only.
 *
 * URL convention (matches every existing inline module):
 * - add/update -> POST ${prefix}/add | /update   (body = form)
 * - delete     -> DELETE ${prefix}/delete          (id as query param)
 * - getById    -> GET  ${prefix}/get_by_id          (id as query param)
 * - list       -> POST ${prefix}/list               (body = page query)
 *
 * `entity` is optional: most modules split the prefix as `{base, entity}`
 * (e.g. `${API_MANAGER_BASE}/label`), but a few base constants already embed the
 * entity path (e.g. `API_SERVICE_ACCOUNT_BASE = 'api/v3/auth/service_account'`),
 * so those pass `{base}` alone.
 *
 * `list` keeps a generic override (`<T = PageResult<TRecord>>`) so callers
 * that remap the envelope (e.g. `listPoint<ListPageResponse>`) still compile.
 */
export type CrudApi<TForm, TRecord> = {
  add: (form: TForm) => Promise<TRecord>;
  update: (form: TForm) => Promise<TRecord>;
  delete: (id: string) => Promise<void>;
  getById: (id: string) => Promise<TRecord>;
  list: <T = PageResult<TRecord>>(query: PageQuery) => Promise<T>;
};

export function createCrudApi<TForm, TRecord>(config: { base: string; entity?: string }): CrudApi<TForm, TRecord> {
  const prefix = config.entity ? `${config.base}/${config.entity}` : config.base;
  return {
    add: (form: TForm) => httpPost<TRecord>(`${prefix}/add`, form),
    update: (form: TForm) => httpPost<TRecord>(`${prefix}/update`, form),
    delete: (id: string) => httpDelete<void>(`${prefix}/delete`, {params: {id}}),
    getById: (id: string) => httpGet<TRecord>(`${prefix}/get_by_id`, {params: {id}}),
    list: <T = PageResult<TRecord>>(query: PageQuery) => httpPost<T>(`${prefix}/list`, query)
  };
}
