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

// Input-modality detection (A3): interaction capability is a runtime
// property of the device, not of the viewport width. A tablet with a
// keyboard+trackpad reports pointer:fine + hover:hover and deserves mouse
// interactions; a phone reports coarse + hover:none and needs thumb
// interactions (44px targets, bottom sheets).

import {onMounted, reactive, readonly} from 'vue';

export interface PointerCapability {
  /** Primary pointer is low-precision (touch). */
  coarse: boolean;
  /** A high-precision pointer (mouse/trackpad/stylus) is present. */
  fine: boolean;
  /** Primary pointer cannot hover. */
  hoverNone: boolean;
  /** Thumb-oriented device: coarse pointer without hover. */
  touch: boolean;
}

const DEFAULTS: PointerCapability = {
  coarse: false,
  fine: true,
  hoverNone: false,
  touch: false,
};

/**
 * Reactive pointer/hover capability. Resolved once on mount — capability is
 * stable for the lifetime of a device session (peripheral hot-plug between
 * mounts is picked up by the next mount).
 */
export const usePointerCapability = () => {
  const state = reactive<PointerCapability>({...DEFAULTS});

  onMounted(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return;
    const fine = window.matchMedia('(pointer: fine)').matches;
    const coarse = window.matchMedia('(pointer: coarse)').matches;
    const hoverNone = window.matchMedia('(hover: none)').matches;
    // Coarse without hover is the canonical thumb device. A coarse pointer
    // WITH hover (rare pen/stylus setups) still prefers hover interactions.
    state.fine = fine;
    state.coarse = coarse;
    state.hoverNone = hoverNone;
    state.touch = coarse && hoverNone;
  });

  return readonly(state);
};
