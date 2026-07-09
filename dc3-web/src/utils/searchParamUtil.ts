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

export type SearchFormData = Record<string, any>;

export const cleanSearchParams = <T extends SearchFormData>(data: T): Partial<T> => {
  const params = {...data};
  Object.keys(params).forEach((key) => {
    if (
      params[key] === '' ||
      params[key] === undefined ||
      params[key] === null ||
      (Array.isArray(params[key]) && params[key].length === 0)
    ) {
      delete params[key];
    }
  });
  return params;
};

export const resetSearchForm = (formData: SearchFormData, defaults: SearchFormData = {}) => {
  Object.keys(formData).forEach((key) => delete formData[key]);
  Object.assign(formData, defaults);
};
