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

/** Login information */
export interface Login {
  tenant?: string;
  name?: string;
  salt?: string;
  password?: string;
  token?: string;
  newPassword?: string;
}

/** Attribute information */
export interface Attribute {
  id: string;
  version: number;
  name: string;
  attributeName: string;
  attributeCode: string;
  attributeTypeFlag?: 'STRING' | 'BYTE' | 'SHORT' | 'INT' | 'LONG' | 'FLOAT' | 'DOUBLE' | 'BOOLEAN';
  defaultValue?: string;
  remark?: string;
  attributeExt?: Record<string, unknown>;
  enableFlag?: string;

  [key: string]: unknown;
}

/** Dictionary item */
export interface Dictionary {
  type?: string;
  label: string;
  value: string;
  disabled: boolean;
  expand: boolean;
  children: Array<Dictionary>;
}

/** Sort order accepted by the server. The field is mapped through a backend whitelist. */
export interface SortSpec {
  field: string;
  direction: 'ASC' | 'DESC';
}

/** Legacy UI sort shape kept only for local table state. It is never sent on the wire. */
export interface Order {
  column: string;
  asc: boolean;
}

/** Offset pagination request. Search fields are carried at the top level. */
export interface PageQuery {
  offset?: number;
  limit?: number;
  sort?: SortSpec[];

  [key: string]: unknown;
}

/** Cursor pagination request for history/high-volume endpoints. */
export interface CursorPageQuery {
  cursor?: string;
  limit?: number;
  sort?: SortSpec[];

  [key: string]: unknown;
}

/** Generic offset paginated response returned by list endpoints. */
export interface PageResult<T = unknown> {
  items: T[];
  offset: number;
  limit: number;
  total: number;
  hasNext: boolean;
}

/** Cursor page returned by history endpoints. The cursor is opaque and signed. */
export interface CursorPageResult<T = unknown> {
  items: T[];
  nextCursor: string | null;
  hasNext: boolean;
}
