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

// App-level preferences: theme + density (A6 — preferences survive device
// and session boundaries; persisted via storageUtil like the auth locale).

import {defineStore} from 'pinia';
import {computed, ref, watch} from 'vue';

import {getStorage, setStorage} from '@/utils/storageUtil';

export type ThemeMode = 'light' | 'dark' | 'auto';
export type Density = 'comfortable' | 'compact';

const THEME_KEY = 'dc3.app.theme';
const DENSITY_KEY = 'dc3.app.density';
const SETTINGS_COLLAPSED_KEY = 'dc3.app.settingsCollapsed';

const THEME_MODES: ThemeMode[] = ['light', 'dark', 'auto'];

const readTheme = (): ThemeMode => {
  const stored = getStorage(THEME_KEY);
  return THEME_MODES.includes(stored as ThemeMode) ? (stored as ThemeMode) : 'auto';
};

const readDensity = (): Density => {
  return getStorage(DENSITY_KEY) === 'compact' ? 'compact' : 'comfortable';
};

/** Pinia store for app-wide UI state: theme mode, density, and settings-sidebar collapse. */
export const useAppStore = defineStore('app', () => {
  // State
  const themeMode = ref<ThemeMode>(readTheme());
  const density = ref<Density>(readDensity());
  const settingsCollapsed = ref(getStorage(SETTINGS_COLLAPSED_KEY) === true);
  const systemDark = ref(false);

  let systemMedia: MediaQueryList | undefined;

  // Getters
  const resolvedTheme = computed(() => (themeMode.value === 'auto' ? (systemDark.value ? 'dark' : 'light') : themeMode.value));
  const isDark = computed(() => resolvedTheme.value === 'dark');
  /** Element Plus global component size derived from density. */
  const componentSize = computed(() => (density.value === 'compact' ? 'small' : 'default'));

  // Actions
  const applyTheme = () => {
    if (typeof document === 'undefined') return;
    const dark = isDark.value;
    document.documentElement.classList.toggle('dark', dark);
    // Native widgets (scrollbars, inputs, prefers-color-scheme consumers).
    document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
  };

  /** Registers the system-preference listener (call once, from main.ts). */
  const init = () => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return;
    systemMedia = window.matchMedia('(prefers-color-scheme: dark)');
    systemDark.value = systemMedia.matches;
    systemMedia.addEventListener('change', (event) => {
      systemDark.value = event.matches;
    });
    applyTheme();
  };

  const setTheme = (mode: ThemeMode) => {
    themeMode.value = mode;
    setStorage(THEME_KEY, mode);
  };

  const setDensity = (value: Density) => {
    density.value = value;
    setStorage(DENSITY_KEY, value);
  };

  const toggleSettingsCollapsed = () => {
    settingsCollapsed.value = !settingsCollapsed.value;
    setStorage(SETTINGS_COLLAPSED_KEY, settingsCollapsed.value);
  };

  watch([resolvedTheme, density], () => {
    applyTheme();
  });

  return {
    // State
    themeMode,
    density,
    settingsCollapsed,
    systemDark,
    // Getters
    resolvedTheme,
    isDark,
    componentSize,
    // Actions
    init,
    setTheme,
    setDensity,
    toggleSettingsCollapsed,
  };
});
