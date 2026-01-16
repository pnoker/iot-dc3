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

import { isNull } from '@/utils/ValidationUtil';

/**
 * Find label by dictionary value
 *
 * @param dic Dictionary array
 * @param value Value to find
 * @returns {string|*} Label string
 */
export const findDicLabel = (dic: any[], value: any): any => {
  let result: any = '';
  if (isNull(dic)) return value;
  if (typeof value == 'string' || typeof value == 'number' || typeof value == 'boolean') {
    let index = 0;
    index = findDicIndex(dic, value);
    if (index !== -1) {
      result = dic[index].label;
    } else {
      result = value;
    }
  } else if (value instanceof Array) {
    result = [];
    let index = 0;
    value.forEach((ele) => {
      index = findDicIndex(dic, ele);
      if (index !== -1) {
        result.push(dic[index].label);
      } else {
        result.push(value);
      }
    });
    result = result.toString();
  }
  return result;
};

/**
 * Find index by dictionary value
 *
 * @param dic Dictionary array
 * @param value Value to find
 * @returns {number} Index of found item, -1 if not found
 */
export const findDicIndex = (dic: any[], value: any): number => {
  for (let i = 0; i < dic.length; i++) {
    if (dic[i].value === value) {
      return i;
    }
  }
  return -1;
};

/**
 * Recursively find parent menu item
 *
 * @param menu Menu array
 * @param id Item id
 * @returns {undefined|*} Parent menu item
 */
export const findParent = (menu: any[], id: string): any => {
  for (let i = 0; i < menu.length; i++) {
    if (menu[i].children.length !== 0) {
      for (let j = 0; j < menu[i].children.length; j++) {
        if (menu[i].children[j].id === id) {
          return menu[i];
        } else {
          if (menu[i].children[j].children.length !== 0) {
            return findParent(menu[i].children[j].children, id);
          }
        }
      }
    }
  }
};
