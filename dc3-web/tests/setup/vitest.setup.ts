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

import {afterEach, beforeEach, vi} from 'vitest';

class ResizeObserverMock {
  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();
}

// Promote Vue template warnings to test failures so silent regressions
// (typo'd refs, missing props, deprecated lifecycle hooks) surface in CI
// instead of disappearing into the console buffer. Set
// VITEST_ALLOW_VUE_WARN=1 locally to opt out of the strict check while
// debugging a noisy test.
const STRICT_VUE_WARN = process.env.VITEST_ALLOW_VUE_WARN !== '1';

// Patterns that we deliberately tolerate — usually third-party stubs that
// emit unavoidable warnings under happy-dom. Keep this list short; prefer
// fixing the underlying issue.
const VUE_WARN_ALLOWLIST: RegExp[] = [/\[Vue warn\]: Failed setting prop "modelValue"/];

const originalWarn = console.warn;
const originalError = console.error;

beforeEach(() => {
  if (!STRICT_VUE_WARN) return;

  console.warn = (...args: unknown[]) => {
    const msg = args.map((a) => (typeof a === 'string' ? a : '')).join(' ');
    if (msg.startsWith('[Vue warn]') && !VUE_WARN_ALLOWLIST.some((re) => re.test(msg))) {
      throw new Error(`Unexpected Vue warning: ${msg}`);
    }
    originalWarn(...args);
  };
  console.error = (...args: unknown[]) => {
    const msg = args.map((a) => (typeof a === 'string' ? a : '')).join(' ');
    if (msg.startsWith('[Vue warn]') && !VUE_WARN_ALLOWLIST.some((re) => re.test(msg))) {
      throw new Error(`Unexpected Vue error: ${msg}`);
    }
    originalError(...args);
  };
});

Object.defineProperty(window, 'ResizeObserver', {
  configurable: true,
  writable: true,
  value: ResizeObserverMock,
});

Object.defineProperty(window, 'matchMedia', {
  configurable: true,
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

Object.defineProperty(window, 'scrollTo', {
  configurable: true,
  writable: true,
  value: vi.fn(),
});

Object.defineProperty(URL, 'createObjectURL', {
  configurable: true,
  writable: true,
  value: vi.fn(() => 'blob:vitest-object-url'),
});

Object.defineProperty(URL, 'revokeObjectURL', {
  configurable: true,
  writable: true,
  value: vi.fn(),
});

// Polyfill localStorage / sessionStorage for Node ≥ 22 where the native
// globals are gated behind --localstorage-file. happy-dom provides them on
// its own Window, but vitest's globalThis may still resolve to Node's
// undefined getter, which breaks source modules that reference
// window.localStorage directly (e.g. storageUtil).
const ensureStorage = (name: 'localStorage' | 'sessionStorage') => {
  if ((globalThis as any)[name] == null) {
    const store = new Map<string, string>();
    const storage = {
      getItem: (key: string) => store.get(key) ?? null,
      setItem: (key: string, value: string) => {
        store.set(key, String(value));
      },
      removeItem: (key: string) => {
        store.delete(key);
      },
      clear: () => {
        store.clear();
      },
      get length() {
        return store.size;
      },
      key: (index: number) => [...store.keys()][index] ?? null,
    };
    Object.defineProperty(globalThis, name, {value: storage, configurable: true, writable: true});
  }
};
ensureStorage('localStorage');
ensureStorage('sessionStorage');

afterEach(() => {
  document.body.innerHTML = '';
  localStorage.clear();
  sessionStorage.clear();
  console.warn = originalWarn;
  console.error = originalError;
});
