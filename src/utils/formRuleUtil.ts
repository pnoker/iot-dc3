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

import type { FormItemRule } from 'element-plus';
import type { ComposerTranslation } from 'vue-i18n';

/** Backend Pattern: ^[A-Za-z0-9\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5-_#@/.|]{1,31}$ */
export const NAME_PATTERN = /^[A-Za-z0-9\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5\-_#@/.|]{1,31}$/;
/** Backend Pattern: ^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$ (no Chinese) */
export const AUTH_NAME_PATTERN = /^[A-Za-z0-9][A-Za-z0-9\-_#@/.|]{1,31}$/;
/** Backend Pattern: ^1([3-9])\d{9}$ */
export const PHONE_PATTERN = /^1([3-9])\d{9}$/;
/** Backend Pattern: ^[A-Za-z0-9_.-]+@[A-Za-z0-9]+\.[A-Za-z0-9]+$ */
export const EMAIL_PATTERN = /^[A-Za-z0-9_.\-]+@[A-Za-z0-9]+\.[A-Za-z0-9]+$/;
export const DECIMAL_PATTERN = /^-?(([0-9]*(\.[0-9]{1,3})$)|([0-9]+$))/;

export function nameRules(t: ComposerTranslation, entityName: string): FormItemRule[] {
  return [
    { required: true, message: t('common.nameRequired', { name: entityName }), trigger: 'blur' },
    { min: 2, max: 32, message: t('common.nameLength'), trigger: 'blur' },
    { pattern: NAME_PATTERN, message: t('common.nameFormat') },
  ];
}

export function remarkRules(t: ComposerTranslation): FormItemRule[] {
  return [{ max: 300, message: t('common.remarkLength'), trigger: 'blur' }];
}
