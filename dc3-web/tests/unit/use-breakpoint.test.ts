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

// L2 breakpoint / pointer-capability composables — the device-class switch
// contract documented in docs/design/frontend-three-terminal-ux.md.

import {mount} from '@vue/test-utils';
import {createPinia, setActivePinia} from 'pinia';
import {defineComponent, h} from 'vue';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {BREAKPOINTS, deviceClassOf, downQuery, type Breakpoint} from '@/config/constant/breakpoints';
import {useBreakpoint} from '@/composables/useBreakpoint';
import {usePointerCapability} from '@/composables/usePointerCapability';
import {useAppStore} from '@/store/modules/app';
import {getStorage} from '@/utils/storageUtil';

/** Install a window.matchMedia mock that answers queries via the matcher. */
const installMatchMedia = (matcher: (query: string) => boolean) => {
  const impl = vi.fn((query: string) => ({
    matches: matcher(query),
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }));
  vi.stubGlobal('matchMedia', impl);
  Object.defineProperty(window, 'matchMedia', {configurable: true, writable: true, value: impl});
  return impl;
};

/** Runs a setup-style composable inside a mounted component instance. */
const mountComposable = <T>(setup: () => T) => {
  let captured!: T;
  const Component = defineComponent({
    setup() {
      captured = setup();
      return () => h('div');
    },
  });
  const wrapper = mount(Component);
  return {wrapper, state: () => captured};
};

/** Answers queries by parsing the px range, treating "viewport" as px width. */
const viewportMatcher = (viewport: number) => (query: string) => {
  if (!query.includes('px')) return false;
  const numbers = query.match(/\d+(?:\.\d+)?px/g)?.map((n) => parseFloat(n)) || [];
  if (numbers.length === 2) return viewport >= numbers[0] && viewport <= numbers[1];
  if (query.startsWith('(max-width')) return viewport <= numbers[0];
  if (query.startsWith('(min-width')) return viewport >= numbers[0];
  return false;
};

describe('breakpoints contract', () => {
  it('downQuery builds inclusive max-width queries with boundary guards', () => {
    expect(downQuery('sm')).toBe('(max-width: 767.98px)');
    expect(downQuery('xl')).toBe('(max-width: 1919.98px)');
  });

  it('maps tiers to device classes', () => {
    expect(deviceClassOf('xs')).toBe('mobile');
    expect(deviceClassOf('sm')).toBe('tablet');
    expect(deviceClassOf('md')).toBe('tablet');
    expect(deviceClassOf('lg')).toBe('desktop');
    expect(deviceClassOf('xl')).toBe('desktop');
  });

  it('keeps BREAKPOINTS aligned with Element Plus el-col semantics', () => {
    expect(BREAKPOINTS).toEqual({xs: 0, sm: 768, md: 992, lg: 1200, xl: 1920});
  });
});

describe('useBreakpoint', () => {
  it.each([
    [375, 'xs', 'mobile'],
    [767, 'xs', 'mobile'],
    [768, 'sm', 'tablet'],
    [1024, 'md', 'tablet'],
    [1200, 'lg', 'desktop'],
    [1920, 'xl', 'desktop'],
  ])('resolves viewport %ipx to %s / %s', (viewport, expectedTier, expectedDevice) => {
    installMatchMedia(viewportMatcher(viewport));
    const {state, wrapper} = mountComposable(() => useBreakpoint());

    expect(state().current.value).toBe(expectedTier as Breakpoint);
    expect(state().device.value).toBe(expectedDevice);
    wrapper.unmount();
  });

  it('exposes per-terminal flags', () => {
    installMatchMedia(viewportMatcher(375));
    const {state, wrapper} = mountComposable(() => useBreakpoint());

    expect(state().isMobile.value).toBe(true);
    expect(state().isTablet.value).toBe(false);
    expect(state().isDesktop.value).toBe(false);
    wrapper.unmount();
  });

  it('keeps per-tier refs mutually exclusive', () => {
    installMatchMedia(viewportMatcher(1440));
    const {state, wrapper} = mountComposable(() => useBreakpoint());

    const active = (Object.keys(state().is) as Breakpoint[]).filter((tier) => state().is[tier].value);
    expect(active).toEqual(['lg']);
    wrapper.unmount();
  });
});

describe('usePointerCapability', () => {
  it('reports a thumb device for coarse + hover:none', () => {
    installMatchMedia((q) => (q.includes('coarse') ? true : q.includes('fine') ? false : q.includes('hover: none')));
    const {state, wrapper} = mountComposable(() => usePointerCapability());

    expect(state().coarse).toBe(true);
    expect(state().fine).toBe(false);
    expect(state().hoverNone).toBe(true);
    expect(state().touch).toBe(true);
    wrapper.unmount();
  });

  it('reports a mouse device for fine pointer with hover', () => {
    installMatchMedia((q) => (q.includes('coarse') ? false : q.includes('fine') ? true : false));
    const {state, wrapper} = mountComposable(() => usePointerCapability());

    expect(state().coarse).toBe(false);
    expect(state().fine).toBe(true);
    expect(state().hoverNone).toBe(false);
    expect(state().touch).toBe(false);
    wrapper.unmount();
  });
});

describe('useAppStore (theme/density preferences)', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  afterEach(() => {
    document.documentElement.classList.remove('dark');
    document.documentElement.style.colorScheme = '';
  });

  it('defaults to auto theme and comfortable density without storage', () => {
    const store = useAppStore();
    expect(store.themeMode).toBe('auto');
    expect(store.density).toBe('comfortable');
    expect(store.componentSize).toBe('default');
  });

  it('persists theme and density through storageUtil', () => {
    const store = useAppStore();
    store.setTheme('dark');
    store.setDensity('compact');

    expect(getStorage('dc3.app.theme')).toBe('dark');
    expect(getStorage('dc3.app.density')).toBe('compact');
    expect(store.componentSize).toBe('small');
  });

  it('applies dark class and color-scheme when resolved theme is dark', () => {
    installMatchMedia((q) => q.includes('prefers-color-scheme'));
    const store = useAppStore();
    store.init();

    expect(store.systemDark).toBe(true);
    expect(store.resolvedTheme).toBe('dark');
    expect(store.isDark).toBe(true);
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(document.documentElement.style.colorScheme).toBe('dark');
  });

  it('explicit light overrides the dark system preference', () => {
    installMatchMedia((q) => q.includes('prefers-color-scheme'));
    const store = useAppStore();
    store.setTheme('light');
    store.init();

    expect(store.resolvedTheme).toBe('light');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });
});
