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
