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
 * Format timestamp to date string
 *
 * @param timestamp Timestamp string
 * @returns Formatted date string
 */
export const timestamp = (timestamp: string): string => {
  return dateFormat(new Date(timestamp));
};

/**
 * Format a timestamp cell value, falling back to the given placeholder.
 * @param value - value to set
 * @param fallback - value used when the primary lookup misses
 * @returns the formatted timestamp, or the fallback
 */
export const timestampLabel = (value: unknown, fallback = '-'): string => {
  if (value == null || value === '') return fallback;
  return timestamp(String(value)) || fallback;
};

/**
 * Format date to string
 *
 * @param date Date object or string
 * @returns {string} Formatted date string
 */
export function dateFormat(date: Date): string {
  if (date.toString() === 'Invalid Date') return '';

  const pad = (n: number, len = 2) => String(n).padStart(len, '0');
  const y = date.getFullYear();
  const M = date.getMonth() + 1;
  const d = date.getDate();
  const h = date.getHours();
  const m = date.getMinutes();
  const s = date.getSeconds();

  return `${y}-${pad(M)}-${pad(d)} ${pad(h)}:${pad(m)}:${pad(s)}`;
}

