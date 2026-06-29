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

import {watch} from 'vue';
import {createI18n} from 'vue-i18n';

import en from './locales/en';
import zh from './locales/zh';

const STORAGE_KEY = 'locale';
const DEFAULT_LOCALE = 'en';
const SUPPORTED = ['en', 'zh'] as const;

function readLocale(): string {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored && (SUPPORTED as readonly string[]).includes(stored)) {
      return stored;
    }
  } catch {
    // localStorage may be unavailable (private mode, iframe without permission, etc.)
  }
  return DEFAULT_LOCALE;
}

const i18n = createI18n({
  legacy: false,
  locale: readLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  messages: {en, zh},
});

// Persist locale to localStorage whenever it changes, regardless of where the change is triggered.
watch(
  i18n.global.locale,
  (value) => {
    try {
      localStorage.setItem(STORAGE_KEY, value as string);
    } catch {
      // localStorage may be unavailable; fail silently.
    }
  },
  {flush: 'post'}
);

export default i18n;
