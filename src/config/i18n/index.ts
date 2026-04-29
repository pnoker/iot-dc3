import { createI18n } from 'vue-i18n';

import en from './locales/en';
import zh from './locales/zh';

const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('locale') || 'en',
  fallbackLocale: 'en',
  messages: { en, zh },
});

export default i18n;
