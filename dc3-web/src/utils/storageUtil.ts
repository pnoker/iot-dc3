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

import {isNull} from '@/utils/validationUtil';
import {decode, encode} from 'js-base64';
import Cookies from 'js-cookie';

/**
 * Get cookie value by key
 *
 * @param key Cookie key
 * @returns Parsed cookie value
 */
export const getCookies = (key: string) => {
  const cookieString = Cookies.get(key) as string | '';
  return JSON.parse(decode(cookieString));
};

/**
 * Set cookie value
 *
 * @param key Cookie key
 * @param value Value to store
 * @returns Cookie set result
 */
export const setCookies = (key: string, value: unknown) => {
  return Cookies.set(key, encode(JSON.stringify(value)));
};

/**
 * Remove cookie by key
 *
 * @param key Cookie key
 * @returns Cookie remove result
 */
export const removeCookies = (key: string) => {
  return Cookies.remove(key);
};

/**
 * Get storage value by key
 *
 * @param key Storage key
 * @param isSession Whether to use Session Storage (default: Local Storage)
 * @returns Storage value
 */
export const getStorage = (key: string, isSession?: boolean): unknown => {
  let obj: string | null;
  if (isSession) obj = window.sessionStorage.getItem(key);
  else obj = window.localStorage.getItem(key);
  if (isNull(obj)) return;

  let parsed: {dataType: string; content: unknown};
  try {
    parsed = JSON.parse(decode(obj!));
  } catch {
    return obj;
  }

  if (parsed.dataType === 'string') {
    return parsed.content as string;
  } else if (parsed.dataType === 'number') {
    return Number(parsed.content);
  } else if (parsed.dataType === 'boolean') {
    return parsed.content === 'true';
  } else if (parsed.dataType === 'object') {
    return parsed.content;
  }
  return parsed.content;
};

/**
 * Set storage value
 *
 * @param key Storage key
 * @param value Value to store
 * @param isSession Whether to use Session Storage (default: Local Storage)
 */
export const setStorage = (key: string, value: unknown, isSession?: boolean) => {
  const obj = {
    dataType: typeof value,
    content: value,
    type: isSession,
    datetime: new Date().getTime(),
  };
  if (isSession) window.sessionStorage.setItem(key, encode(JSON.stringify(obj)));
  else window.localStorage.setItem(key, encode(JSON.stringify(obj)));
};

/**
 * Remove storage value by key
 *
 * @param key Storage key
 * @param isSession Whether to use Session Storage (default: Local Storage)
 */
export const removeStorage = (key: string, isSession?: boolean) => {
  if (isSession) window.sessionStorage.removeItem(key);
  else window.localStorage.removeItem(key);
};

/**
 * Get all storage values
 *
 * @param isSession Whether to use Session Storage (default: Local Storage)
 * @returns Array of storage items
 */
export const getAllStorage = (isSession?: boolean) => {
  const list = [] as Array<{name: string | null; content: unknown}>;
  if (isSession) {
    for (let i = 0; i < window.sessionStorage.length; i++) {
      list.push({
        name: window.sessionStorage.key(i),
        content: getStorage(window.sessionStorage.key(i) || ''),
      });
    }
  } else {
    for (let i = 0; i < window.localStorage.length; i++) {
      list.push({
        name: window.localStorage.key(i),
        content: getStorage(window.localStorage.key(i) || ''),
      });
    }
  }
  return list;
};

/**
 * Clear all storage values
 *
 * @param isSession Whether to use Session Storage (default: Local Storage)
 */
export const clearAllStorage = (isSession?: boolean) => {
  if (isSession) {
    window.sessionStorage.clear();
  } else {
    window.localStorage.clear();
  }
};
