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

// Breakpoint state for coarse device-class switching (A2). Components use
// this for conditional rendering (e.g. table vs card list); layout that only
// needs CSS keeps using el-col responsive props / media queries.

import {computed, type ComputedRef, type Ref} from 'vue';

import {BREAKPOINTS, deviceClassOf, downQuery, type Breakpoint, type DeviceClass} from '@/config/constant/breakpoints';
import {useMediaQuery} from '@/composables/useMediaQuery';

export interface BreakpointState {
  /** Current tier — tiers are mutually exclusive (xs < sm < md < lg < xl). */
  current: ComputedRef<Breakpoint>;
  /** Device class of the current tier: mobile / tablet / desktop. */
  device: ComputedRef<DeviceClass>;
  /** One ref per tier; the tier is active when the viewport is inside it. */
  is: Record<Breakpoint, Ref<boolean>>;
  /** Convenience flags for the three terminals. */
  isMobile: ComputedRef<boolean>;
  isTablet: ComputedRef<boolean>;
  isDesktop: ComputedRef<boolean>;
}

/**
 * Reactive breakpoint state, derived from the single BREAKPOINTS contract.
 *
 * Tiers mirror Element Plus el-col: xs <768, sm 768-991, md 992-1199,
 * lg 1200-1919, xl >=1920. Terminal mapping: xs=mobile, sm/md=tablet,
 * lg/xl=desktop.
 */
export const useBreakpoint = (): BreakpointState => {
  const isXs = useMediaQuery(downQuery('sm'));
  const isSm = useMediaQuery(`(min-width: ${BREAKPOINTS.sm}px) and ${downQuery('md')}`);
  const isMd = useMediaQuery(`(min-width: ${BREAKPOINTS.md}px) and ${downQuery('lg')}`);
  const isLg = useMediaQuery(`(min-width: ${BREAKPOINTS.lg}px) and ${downQuery('xl')}`);
  const isXl = useMediaQuery(`(min-width: ${BREAKPOINTS.xl}px)`);

  const current = computed<Breakpoint>(() => {
    if (isXl.value) return 'xl';
    if (isLg.value) return 'lg';
    if (isMd.value) return 'md';
    if (isSm.value) return 'sm';
    return 'xs';
  });

  const device = computed<DeviceClass>(() => deviceClassOf(current.value));

  return {
    current,
    device,
    is: {xs: isXs, sm: isSm, md: isMd, lg: isLg, xl: isXl},
    isMobile: computed(() => device.value === 'mobile'),
    isTablet: computed(() => device.value === 'tablet'),
    isDesktop: computed(() => device.value === 'desktop'),
  };
};
