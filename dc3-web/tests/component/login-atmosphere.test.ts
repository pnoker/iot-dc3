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

import {mount} from '@vue/test-utils';
import {afterAll, beforeAll, describe, expect, it, vi} from 'vitest';

import LoginAtmosphere from '@/components/login-atmosphere/LoginAtmosphere.vue';

// The atmosphere paints into a 2d canvas via requestAnimationFrame; the test
// environment has no real canvas or observer implementations. Stub them so the
// component mounts and its guards (null context, hidden document) keep the
// paint loop inert.
beforeAll(() => {
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockImplementation(() => null);
  vi.stubGlobal('requestAnimationFrame', vi.fn());
  vi.stubGlobal(
    'IntersectionObserver',
    class {
      observe() {}

      unobserve() {}

      disconnect() {}
    }
  );
  vi.stubGlobal(
    'ResizeObserver',
    class {
      observe() {}

      unobserve() {}

      disconnect() {}
    }
  );
});

afterAll(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('LoginAtmosphere', () => {
  it('mounts its canvas layer without throwing when canvas is unavailable', () => {
    const wrapper = mount(LoginAtmosphere);
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find('.login-atmosphere').exists()).toBe(true);
    expect(wrapper.find('canvas').exists()).toBe(true);
    wrapper.unmount();
  });

  it('keeps the background layer non-interactive for the login form', () => {
    const wrapper = mount(LoginAtmosphere);
    // pointer-events: none lives in the scoped style; the contract the login
    // page relies on is that the atmosphere never captures form clicks.
    const style = wrapper.find('.login-atmosphere').attributes('style');
    expect(style).toBeUndefined();
    wrapper.unmount();
  });
});
