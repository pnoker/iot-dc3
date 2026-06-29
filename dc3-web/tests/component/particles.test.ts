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

import {mount} from '@vue/test-utils';
import {afterAll, beforeAll, describe, expect, it, vi} from 'vitest';

import Particles from '@/components/particles/Particles.vue';

// Particles paints into a 3D canvas via requestAnimationFrame; jsdom has
// no real canvas. Stub HTMLCanvasElement.prototype.getContext to keep the
// initial paint loop from throwing while still letting the component mount.
beforeAll(() => {
  // happy-dom's HTMLCanvasElement has no useful getContext implementation;
  // the component swallows null returns and skips the WebGL setup. Use a
  // typed function override (compatible with the wide overload set) so we
  // don't have to launder the return through a double assertion.
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockImplementation(() => null);
  // happy-dom doesn't have rAF — provide a no-op so the animation loop doesn't crash.
  vi.stubGlobal('requestAnimationFrame', vi.fn());
});

afterAll(() => {
  vi.unstubAllGlobals();
});

describe('Particles', () => {
  it('mounts without throwing even when canvas getContext returns null', () => {
    const wrapper = mount(Particles);
    expect(wrapper.exists()).toBe(true);
    // Particles renders a `.waves` host div; the canvas itself is created
    // and appended by the embedded ShaderProgram at runtime — in jsdom
    // with a stubbed getContext that work is a no-op, so we just assert
    // the host element survives mount.
    expect(wrapper.find('.waves').exists()).toBe(true);
  });
});
