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

import type {FormItemRule} from 'element-plus';
import type {ComposerTranslation} from 'vue-i18n';

const NAME_MIN_LENGTH = 2;
const NAME_MAX_LENGTH = 32;
const REMARK_MAX_LENGTH = 300;
const BYTE_MIN = 0;
const BYTE_MAX = 127;

/** Backend Pattern: ^[A-Za-z0-9\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5-_#@/.|]{1,31}$ */
export const NAME_PATTERN = /^[A-Za-z0-9\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5\-_#@/.|]{1,31}$/;
/** Backend Pattern: ^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$ (no Chinese) */
export const AUTH_NAME_PATTERN = /^[A-Za-z0-9][A-Za-z0-9\-_#@/.|]{1,31}$/;
/** Backend Pattern: ^1([3-9])\d{9}$ */
export const PHONE_PATTERN = /^1([3-9])\d{9}$/;
/** Backend Pattern: ^[A-Za-z0-9_.-]+@[A-Za-z0-9]+\.[A-Za-z0-9]+$ */
export const EMAIL_PATTERN = /^[A-Za-z0-9_.\-]+@[A-Za-z0-9]+\.[A-Za-z0-9]+$/;
/** Backend Pattern: IPv4 address. */
export const SERVICE_HOST_PATTERN = /^((2(5[0-5]|[0-4]\d))|[0-1]?\d{1,2})(\.((2(5[0-5]|[0-4]\d))|[0-1]?\d{1,2})){3}$/;
export const DECIMAL_PATTERN = /^-?(?:\d+|\d*\.\d{1,3})$/;
export const INTEGER_PATTERN = /^-?\d+$/;
export const UNSIGNED_INTEGER_PATTERN = /^\d+$/;

function empty(value: unknown): boolean {
  return value === undefined || value === null || value === '';
}

export function nameRules(t: ComposerTranslation, entityName: string): FormItemRule[] {
  return [
    {required: true, whitespace: true, message: t('common.nameRequired', {name: entityName}), trigger: 'blur'},
    {min: NAME_MIN_LENGTH, max: NAME_MAX_LENGTH, message: t('common.nameLength'), trigger: 'blur'},
    {pattern: NAME_PATTERN, message: t('common.nameFormat'), trigger: 'blur'},
  ];
}

export function authNameRules(t: ComposerTranslation, entityName: string): FormItemRule[] {
  return [
    {required: true, whitespace: true, message: t('common.nameRequired', {name: entityName}), trigger: 'blur'},
    {min: NAME_MIN_LENGTH, max: NAME_MAX_LENGTH, message: t('common.authNameLength'), trigger: 'blur'},
    {pattern: AUTH_NAME_PATTERN, message: t('common.authNameFormat'), trigger: 'blur'},
  ];
}

export function remarkRules(t: ComposerTranslation): FormItemRule[] {
  return [{max: REMARK_MAX_LENGTH, message: t('common.remarkLength'), trigger: 'blur'}];
}

export function requiredStringRule(message: string, trigger: 'blur' | 'change' = 'blur'): FormItemRule[] {
  return [{required: true, whitespace: true, message, trigger}];
}

export function requiredSelectRule(message: string): FormItemRule[] {
  return [{required: true, message, trigger: 'change'}];
}

export function decimalRules(message: string, requiredMessage?: string): FormItemRule[] {
  const rules: FormItemRule[] = requiredMessage ? requiredStringRule(requiredMessage) : [];
  rules.push({
    validator: (_rule, value, callback) => {
      if (empty(value)) {
        callback();
        return;
      }
      if (!DECIMAL_PATTERN.test(String(value))) {
        callback(new Error(message));
        return;
      }
      callback();
    },
    trigger: 'blur',
  });
  return rules;
}

export function byteRules(t: ComposerTranslation, requiredMessage: string): FormItemRule[] {
  return [
    {required: true, message: requiredMessage, trigger: 'blur'},
    {
      validator: (_rule, value, callback) => {
        if (empty(value)) {
          callback();
          return;
        }

        const numericValue = Number(value);
        if (!Number.isInteger(numericValue) || numericValue < BYTE_MIN || numericValue > BYTE_MAX) {
          callback(new Error(t('common.byteRange', {min: BYTE_MIN, max: BYTE_MAX})));
          return;
        }

        callback();
      },
      trigger: 'blur',
    },
  ];
}

export function positiveIntegerRules(t: ComposerTranslation, requiredMessage: string): FormItemRule[] {
  return [
    {required: true, message: requiredMessage, trigger: 'blur'},
    {
      validator: (_rule, value, callback) => {
        if (empty(value)) {
          callback();
          return;
        }

        const text = String(value);
        if (!UNSIGNED_INTEGER_PATTERN.test(text) || Number(text) < 1) {
          callback(new Error(t('common.positiveIntegerFormat')));
          return;
        }

        callback();
      },
      trigger: 'blur',
    },
  ];
}
