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

/**
 * Lightweight locale loader — reads from locales/{lang}.json at config time.
 * No runtime dependency; strings are resolved statically during VitePress build.
 *
 * Adding a language:
 *   1. Create `locales/<lang>.json` with the same keys as `zh.json`.
 *   2. Add the lang to the `Lang` type union.
 *   3. Register the locale in VitePress `locales` config block.
 */

import zh from '../locales/zh.json'
import en from '../locales/en.json'

export type Lang = 'zh' | 'en'

const bundles: Record<Lang, Record<string, string>> = {zh, en}

/** Resolve a translation key for the given language. Falls back to zh if key or lang is missing. */
export function t(lang: Lang, key: string): string {
    return bundles[lang]?.[key] ?? bundles.zh[key] ?? key
}
