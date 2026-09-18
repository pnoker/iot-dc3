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

// The single breakpoint contract shared by useBreakpoint and el-col
// responsive props (docs/design/frontend-three-terminal-ux.md, A5).
// Keep in lockstep with $breakpoint-* in src/styles/tokens.scss.

export type Breakpoint = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

/** Lower bound of each tier in px, matching Element Plus el-col semantics. */
export const BREAKPOINTS: Record<Breakpoint, number> = {
  xs: 0,
  sm: 768,
  md: 992,
  lg: 1200,
  xl: 1920,
};

/** Tier order, narrowest first — used for "current breakpoint" resolution. */
export const BREAKPOINT_ORDER: Breakpoint[] = ['xs', 'sm', 'md', 'lg', 'xl'];

/** Device class derived from a breakpoint: mobile / tablet / desktop. */
export type DeviceClass = 'mobile' | 'tablet' | 'desktop';

export const deviceClassOf = (breakpoint: Breakpoint): DeviceClass => {
  if (breakpoint === 'xs') return 'mobile';
  if (breakpoint === 'sm' || breakpoint === 'md') return 'tablet';
  return 'desktop';
};

/** media query string for "this tier and narrower" (max-width inclusive). */
export const downQuery = (breakpoint: Exclude<Breakpoint, 'xs'>): string => {
  const max = BREAKPOINTS[breakpoint] - 0.02;
  return `(max-width: ${max}px)`;
};
