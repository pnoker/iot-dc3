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
 * Formatting helpers shared by the point dashboard cards: numeric display,
 * axis-label compaction, interval-bin labels, bucket hour labels and the
 * CSS-variable colour resolution G2 needs before painting. Kept local to
 * the dashboard folder — these are display concerns of this page only.
 */

/**
 * Trim trailing zeros of a fixed-decimal string ("42.50" → "42.5").
 * @param raw - raw fixed-decimal string
 * @returns the trimmed decimal string
 */
const trimZero = (raw: string): string => {
  if (!raw.includes('.')) return raw;
  const trimmed = raw.replace(/0+$/, '').replace(/\.$/, '');
  return trimmed || '0';
};

/**
 * Human numeric value for KPI cells and tooltips. Null/NaN degrade to '--'
 * so a partially-populated payload never renders "NaN". Integers render
 * without decimals; fractions keep up to 4 significant decimal digits
 * (history values may carry small precision).
 * @param v - numeric value to format
 * @returns the formatted value string
 */
export const formatValue = (v: number | null | undefined): string => {
  if (v == null || !Number.isFinite(v)) return '--';
  if (Number.isInteger(v)) return v.toLocaleString('en-US');
  return v.toLocaleString('en-US', {maximumFractionDigits: 4});
};

/**
 * Compact axis label: 1.2k / 3.4M for large magnitudes, plain otherwise.
 * @param v - numeric value to compact
 * @returns the compact label string
 */
export const compactNumber = (v: number): string => {
  if (!Number.isFinite(v)) return '--';
  const abs = Math.abs(v);
  if (abs >= 1_000_000) return `${trimZero((v / 1_000_000).toFixed(1))}M`;
  if (abs >= 1_000) return `${trimZero((v / 1_000).toFixed(1))}k`;
  if (abs < 10 && !Number.isInteger(v)) return trimZero(v.toFixed(2));
  return String(v);
};

/**
 * Sample count label; a truncated walk renders with a '>' prefix so the
 * reader sees the count is a lower bound, not an exact total.
 * @param count - sample count to format
 * @param truncated - whether the raw walk hit its cap
 * @returns the count label string
 */
export const formatSampleCount = (count: number, truncated: boolean): string => {
  const body = Number.isFinite(count) ? Math.round(count).toLocaleString('en-US') : '--';
  return truncated && Number.isFinite(count) ? `>${body}` : body;
};

/**
 * Millisecond value as a compact duration token: 500 → '0.5s', 60000 → '1m',
 * 1800000 → '30m'. Values that hit whole units render without decimals.
 * @param ms - millisecond value to shorten
 * @returns the compact duration string
 */
const shortMs = (ms: number): string => {
  if (ms < 1000) return `${trimZero((ms / 1000).toFixed(2))}s`;
  if (ms < 60_000) return `${trimZero((ms / 1000).toFixed(2))}s`;
  if (ms < 3_600_000) return `${trimZero((ms / 60_000).toFixed(2))}m`;
  return `${trimZero((ms / 3_600_000).toFixed(2))}h`;
};

/**
 * Axis label of one sampling-interval bin: the upper bound for closed bins
 * ("1s", "30m"), or ">30m" for the open-ended top bin (toMs = null).
 * @param fromMs - lower bound in milliseconds (inclusive)
 * @param toMs - upper bound in milliseconds (exclusive); null opens the top bin
 * @returns the bin label string
 */
export const intervalBinLabel = (fromMs: number, toMs: number | null): string => {
  if (toMs == null || !Number.isFinite(toMs)) return `>${shortMs(fromMs)}`;
  return shortMs(toMs);
};

/**
 * Hour bucket label for trend/volume axes: "HH:mm" inside a single day,
 * "MM-dd" when the window spans multiple days so repeated hours stay
 * distinguishable.
 * @param iso - ISO instant string of the bucket start
 * @param showDate - whether to prefix the month/day part
 * @returns the axis label string
 */
export const hourLabel = (iso: string, showDate: boolean): string => {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const pad = (n: number) => String(n).padStart(2, '0');
  const clock = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  return showDate ? `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${clock}` : clock;
};

/**
 * Resolve a CSS custom-property colour into the concrete value G2 accepts.
 * G2 paints to canvas and cannot read var(--…) itself; the resolved colour
 * is a snapshot of the active theme at draw time.
 * @param color - CSS colour, possibly a var(--…) token
 * @returns the resolved colour string
 */
export const resolveCssColor = (color: string): string => {
  const match = color.match(/^var\((--[^),]+)(?:,[^)]+)?\)$/);
  const token = match?.[1];
  if (!token) return color;
  return getComputedStyle(document.documentElement).getPropertyValue(token).trim() || color;
};

/**
 * Fixed chart palette matching the Element Plus tone scale already used by
 * the dashboard family (LatencyChart, AlertTypePie). Canvas marks cannot
 * consume CSS variables, so the tones are pinned here.
 * @returns the chart palette record
 */
export const chartPalette = (): {primary: string; success: string; warning: string; danger: string; info: string} => ({
  primary: resolveCssColor('var(--el-color-primary)') || '#409eff',
  success: resolveCssColor('var(--el-color-success)') || '#67c23a',
  warning: resolveCssColor('var(--el-color-warning)') || '#e6a23c',
  danger: resolveCssColor('var(--el-color-danger)') || '#f56c6c',
  info: '#909399',
});
