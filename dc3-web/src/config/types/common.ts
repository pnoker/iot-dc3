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
  name: string;
  attributeName: string;
  attributeCode: string;
  attributeTypeFlag?: 'STRING' | 'BYTE' | 'SHORT' | 'INT' | 'LONG' | 'FLOAT' | 'DOUBLE' | 'BOOLEAN' | string;
  defaultValue?: string;
  remark?: string;
  attributeExt?: Record<string, unknown>;
  enableFlag?: string;
}

/** Dictionary item */
export interface Dictionary {
  type: string;
  label: string;
  value: string;
  disabled: boolean;
  expand: boolean;
  children: Array<Dictionary>;
}

/** Sort order for table queries */
export interface Order {
  column: string;
  asc: boolean;
}

/** Pagination query shape used by all list endpoints */
export interface PageQuery {
  page?: {
    total?: number;
    size?: number;
    current?: number;
    orders?: Order[];
  };

  [key: string]: unknown;
}

/** Generic paginated response */
export interface PageResult<T = unknown> {
  total: number;
  records: T[];
}
