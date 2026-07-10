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
