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

// Chinese mobile phone prefixes as of 2024+
// 13x, 14x, 15x, 16x, 17x, 18x, 19x
export function isPhone(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone);
}

export function isNum(num: unknown, type: number): boolean {
  if (type === 1) {
    return /^\d*\.?\d+$/.test(String(num));
  }
  if (type === 2) {
    return /^\d+$/.test(String(num));
  }
  return true;
}

export function isNull(val: unknown): boolean {
  if (typeof val === 'boolean') return false;
  if (typeof val === 'number') return false;
  if (Array.isArray(val)) {
    return val.length === 0;
  }
  if (val !== null && typeof val === 'object') {
    return Object.keys(val as object).length === 0;
  }
  return val === 'null' || val == null || val === 'undefined' || val === undefined || val === '';
}
