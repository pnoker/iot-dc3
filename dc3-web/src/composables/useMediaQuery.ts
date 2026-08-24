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

// Reactive matchMedia binding (L2, A2). SSR/mock-safe: without window the
// query reports the default until the component mounts.

import {onBeforeUnmount, onMounted, ref, type Ref} from 'vue';

/**
 * Tracks a CSS media query as a reactive boolean.
 *
 * @param query Media query string, e.g. '(max-width: 767.98px)'
 * @param initial Initial value before mount (SSR / no-window environments)
 */
export const useMediaQuery = (query: string, initial = false): Ref<boolean> => {
  const matches = ref(initial);

  let mql: MediaQueryList | undefined;

  const sync = () => {
    if (mql) matches.value = mql.matches;
  };

  onMounted(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      // Test/build environments without matchMedia keep the initial value.
      return;
    }
    mql = window.matchMedia(query);
    sync();
    mql.addEventListener('change', sync);
  });

  onBeforeUnmount(() => {
    mql?.removeEventListener('change', sync);
  });

  return matches;
};
