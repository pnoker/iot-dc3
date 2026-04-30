import { watch } from 'vue';
import { createI18n } from 'vue-i18n';

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
  messages: { en, zh },
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
  { flush: 'post' }
);

export default i18n;
