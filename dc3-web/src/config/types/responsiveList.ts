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

/**
 * Responsive list cell kind union.
 */
export type ResponsiveListCellKind = 'text' | 'time' | 'tag' | 'enable' | 'default' | 'code' | 'custom';
/**
 * responsive list mobile role type alias.
 */
export type ResponsiveListMobileRole = 'primary' | 'detail' | 'hidden';
/**
 * Responsive list tag type union.
 */
export type ResponsiveListTagType = 'primary' | 'success' | 'info' | 'warning' | 'danger';

/**
 * ResponsiveListColumn data contract.
 */
export interface ResponsiveListColumn<T extends Record<string, any> = Record<string, any>> {
  key: string;
  label: string;
  prop?: string;
  kind?: ResponsiveListCellKind;
  width?: number | string;
  minWidth?: number | string;
  fixed?: boolean | 'left' | 'right';
  overflow?: boolean;
  mobile?: ResponsiveListMobileRole;
  formatter?: (row: T) => string;
  tagType?: (row: T) => ResponsiveListTagType;
}
