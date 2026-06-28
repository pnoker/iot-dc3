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
