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
 * Shared time-formatting helpers. Every dashboard card used to declare its
 * own `formatTime` / `humanDuration` / `formatMs`, each slightly different
 * (some dropped the date, some used toLocaleString, some toLocaleTimeString).
 * Normalised here so one change reaches every card.
 *
 * <p>The Intl locale follows the app i18n language ('zh' → 'zh-CN', otherwise
 * 'en-US') with a fixed 24-hour clock — matching existing display conventions.</p>
 */

import i18n from '@/config/i18n';

/** Resolve the Intl locale tag from the active app locale. */
const LOCALE = (): string => (i18n.global.locale.value === 'zh' ? 'zh-CN' : 'en-US');

/** Parse ISO / "yyyy-MM-dd HH:mm:ss" into a Date; returns null on failure. */
export const parseDateSafe = (v: string | Date | undefined | null): Date | null => {
  if (v == null || v === '') return null;
  if (v instanceof Date) return Number.isNaN(v.getTime()) ? null : v;
  // Backend "2026-05-04 12:34:56" needs the space→T swap to round-trip via
  // the JS Date parser in every browser; ISO-8601 strings already have "T".
  const d = new Date(typeof v === 'string' && v.includes(' ') && !v.includes('T') ? v.replace(' ', 'T') : v);
  return Number.isNaN(d.getTime()) ? null : d;
};

/**
 * Full date + clock — "2026/05/04 12:34:56". Used for audit-style timestamps
 * (alarm create_time, last_seen, operate_time).
 */
export const formatDateTime = (v: string | Date | undefined | null): string => {
  const d = parseDateSafe(v);
  if (!d) return typeof v === 'string' ? v : '';
  return d.toLocaleString(LOCALE(), {hour12: false});
};

/** Clock only — "12:34:56". Used by live feeds where the date context is already implicit. */
export const formatClock = (v: string | Date | undefined | null): string => {
  const d = parseDateSafe(v);
  if (!d) return typeof v === 'string' ? v : '';
  return d.toLocaleTimeString(LOCALE(), {hour12: false});
};

/**
 * Month/day + clock — "05/04 12:34". Used on the timeline recent-unconfirmed
 * cards where space is tight and the year adds no info.
 */
export const formatShortDateTime = (v: string | Date | undefined | null): string => {
  const d = parseDateSafe(v);
  if (!d) return typeof v === 'string' ? v : '';
  return d.toLocaleString(LOCALE(), {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
};

/**
 * Millisecond duration → concise human string: ms / s / m / h. Used for
 * MTTA display where values span 0-3600000+.
 */
export const formatMs = (ms: number | null | undefined): string => {
  if (!ms || ms <= 0) return '—';
  if (ms < 1000) return `${Math.round(ms)}ms`;
  const s = ms / 1000;
  if (s < 60) return `${s.toFixed(1)}s`;
  const m = s / 60;
  if (m < 60) return `${m.toFixed(1)}m`;
  return `${(m / 60).toFixed(1)}h`;
};

/** Seconds → compact duration: "42s" / "3m" / "2h" / "5d". */
export const humanDuration = (seconds: number | null | undefined): string => {
  if (!seconds || seconds < 0) return '—';
  if (seconds < 60) return `${Math.floor(seconds)}s`;
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h`;
  return `${Math.floor(seconds / 86400)}d`;
};
