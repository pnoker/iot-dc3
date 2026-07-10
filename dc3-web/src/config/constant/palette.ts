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

/**
 * Dashboard family palette — the driver/device/profile/point four-layer
 * colours are referenced by the topology Sankey, correlation graph, change
 * impact timeline, SLA badge, live feed and stat chips. Consolidated here
 * so a brand tweak is a single-line change instead of a project-wide
 * find-replace across SCSS + TS + templates.
 *
 * <p>SCSS usages mirror these values in `src/styles/palette.scss` (see
 * also that file) — Vite's ts→scss bridge isn't set up, so the two live
 * in lockstep. Keep them in sync when tuning.</p>
 */

/** Four-layer entity tones. */
export const DASHBOARD_PALETTE = {
  /** Driver — protocol adapter layer. */
  driver: '#9059f6',
  /** Device — physical field equipment. */
  device: '#409eff',
  /** Profile — device model / template. */
  profile: '#e6a23c',
  /** Point — tagged signal. */
  point: '#67c23a',
  /** Others — catch-all for cropped / Top-N overflow buckets. */
  others: '#c0c4cc',
} as const;

export type DashboardPaletteKey = keyof typeof DASHBOARD_PALETTE;

/**
 * Sankey / graph nodes come in as strings; centralise the default→key
 * fallback so each consumer doesn't re-implement the same switch.
 */
export const resolveDashboardColour = (type: string | undefined | null): string => {
  if (!type) return DASHBOARD_PALETTE.others;
  if (type === 'driver') return DASHBOARD_PALETTE.driver;
  if (type === 'device') return DASHBOARD_PALETTE.device;
  if (type === 'profile') return DASHBOARD_PALETTE.profile;
  if (type === 'point') return DASHBOARD_PALETTE.point;
  return DASHBOARD_PALETTE.others;
};

/**
 * Hex → rgba conversion for tinted backgrounds (e.g. the SLA badge chip
 * backgrounds that use the same hue at 12% opacity). Hex must be `#rrggbb`.
 */
export const hexToRgba = (hex: string, alpha: number): string => {
  const normalised = hex.replace('#', '');
  const r = parseInt(normalised.slice(0, 2), 16);
  const g = parseInt(normalised.slice(2, 4), 16);
  const b = parseInt(normalised.slice(4, 6), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
};
