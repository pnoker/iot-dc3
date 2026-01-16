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

/**
 * Check if string is a URL
 *
 * @param url String to check
 * @returns {boolean} Whether string is a URL
 */
export function isUrl(url: any) {
  return /^https?:\/\/.*/.test(url);
}

/**
 * Check if string is an email
 *
 * @param email String to check
 * @returns {boolean} Whether string is an email
 */
export function isEmail(email: any) {
  return /^([a-zA-Z0-9_\\.-])+\\@(([a-zA-Z0-9-])+\\.)+([a-zA-Z0-9]{2,4})+$/.test(email);
}

/**
 * Check if string is a phone number
 *
 * @param phone String to check
 * @returns {boolean} Whether string is a phone number
 */
export function isPhone(phone: any) {
  return /^(13[0-9]|14[5|7]|15[0|1|2|3|4|5|6|7|8|9]|18[0|1|2|3|5|6|7|8|9])\\d{8}$/.test(phone);
}

/**
 * Check if value is a number
 *
 * @param num Value to check
 * @param type Type check (1: positive integer, 2: non-negative integer)
 * @returns {boolean} Whether value is a number
 */
export function isNum(num: any, type: any) {
  let regName = /^[^\d.]/g;
  if (type == 1) {
    if (!regName.test(num)) return false;
  } else if (type == 2) {
    regName = /^[^\d]/g;
    if (!regName.test(num)) return false;
  }
  return true;
}

/**
 * Check if value is a decimal number
 *
 * @param num Value to check
 * @param type Type check (1: positive, 2: non-negative)
 * @returns {boolean} Whether value is a decimal number
 */
export function isNumord(num: any, type: any) {
  let regName = /^[^\d.]/g;
  if (type == 1) {
    if (!regName.test(num)) return false;
  } else if (type == 2) {
    regName = /^[^\d.]/g;
    if (!regName.test(num)) return false;
  }
  return true;
}

/**
 * Check if value is null or empty
 *
 * @param val Value to check
 * @returns {boolean} Whether value is null or empty
 */
export function isNull(val: any) {
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
