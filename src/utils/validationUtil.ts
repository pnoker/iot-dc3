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

export function isUrl(url: string): boolean {
  return /^https?:\/\/.*/.test(url);
}

export function isEmail(email: string): boolean {
  return /^([a-zA-Z0-9_.-])+\@(([a-zA-Z0-9-])+\.)+([a-zA-Z0-9]{2,4})+$/.test(email);
}

export function isPhone(phone: string): boolean {
  return /^(13[0-9]|14[5|7]|15[0|1|2|3|4|5|6|7|8|9]|18[0|1|2|3|5|6|7|8|9])\d{8}$/.test(phone);
}

export function isNum(num: unknown, type: number): boolean {
  let regName = /^[^\d.]/g;
  if (type == 1) {
    if (!regName.test(String(num))) return false;
  } else if (type == 2) {
    regName = /^[^\d]/g;
    if (!regName.test(String(num))) return false;
  }
  return true;
}

export function isNumord(num: unknown, type: number): boolean {
  let regName = /^[^\d.]/g;
  if (type == 1) {
    if (!regName.test(String(num))) return false;
  } else if (type == 2) {
    regName = /^[^\d.]/g;
    if (!regName.test(String(num))) return false;
  }
  return true;
}

export function isNull(val: unknown): boolean {
  if (typeof val == 'boolean') return false;
  if (typeof val == 'number') return false;
  if (val instanceof Array) {
    if (val.length === 0) return true;
  } else if (val instanceof Object) {
    if (JSON.stringify(val) === '{}') return true;
  } else {
    return val === 'null' || val == null || val === 'undefined' || val === undefined || val === '';
  }
  return false;
}
