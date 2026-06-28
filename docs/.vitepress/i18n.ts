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
