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

const POINT_TYPE_KEY: Record<string, string> = {
  STRING: 'dataType.string',
  BYTE: 'dataType.byte',
  SHORT: 'dataType.short',
  INT: 'dataType.int',
  LONG: 'dataType.long',
  FLOAT: 'dataType.float',
  DOUBLE: 'dataType.double',
  BOOLEAN: 'dataType.boolean',
};

const RW_FLAG_KEY: Record<string, string> = {
  R: 'status.readOnly',
  READ_ONLY: 'status.readOnly',
  W: 'status.writeOnly',
  WRITE_ONLY: 'status.writeOnly',
  RW: 'status.readWrite',
  READ_WRITE: 'status.readWrite',
};

// 返回 i18n key 而非翻译后的字符串,让模板用 $t(pointTypeKey(...)) 保持 locale 切换响应式
export function pointTypeKey(flag?: string): string {
  return POINT_TYPE_KEY[flag ?? ''] ?? 'status.unknown';
}

export function rwFlagKey(flag?: string): string {
  return RW_FLAG_KEY[String(flag ?? '').toUpperCase()] ?? 'status.unknown';
}
