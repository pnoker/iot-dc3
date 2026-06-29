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
