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

import i18n from '@/config/i18n';
import type {MenuNode} from '@/store/modules/menu';

/**
 * Resolve the display name of a menu node given the currently active locale.
 *
 * Priority:
 *   1. menuExt.content.titles[currentLocale] — authoritative per-locale
 *      title maintained from the menu CRUD form.
 *   2. menuExt.content.titles.en — English fallback when the locale the
 *      user picked has no explicit entry.
 *   3. Legacy menuExt.content.title — may be either an i18n key (old seed
 *      data like `"nav.home"`) or plain text; fed through $t so existing
 *      `nav.*` keys keep resolving, and falls through to the raw string
 *      when the key is not registered in locales.
 *   4. menuName — the English machine name stored on the row, so the UI
 *      never shows a blank label.
 */
export function resolveMenuTitle(node: Partial<MenuNode> | null | undefined): string {
  if (!node) return '';
  const content = node.menuExt?.content;
  if (content) {
    const locale = i18n.global.locale.value as string;
    const titles = content.titles;
    if (titles) {
      if (titles[locale]) return titles[locale];
      if (titles.en) return titles.en;
      // Any other locale populated on the row — pick the first non-empty
      // entry so a row authored only in e.g. Japanese doesn't render blank.
      for (const key of Object.keys(titles)) {
        if (titles[key]) return titles[key];
      }
    }
    if (content.title) {
      const translated = i18n.global.t(content.title);
      return translated && translated !== content.title ? translated : content.title;
    }
  }
  return node.menuName || '';
}
