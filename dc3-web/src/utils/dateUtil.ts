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
 * el-table-column formatter that renders a timestamp cell in the same
 * `yyyy-MM-dd hh:mm:ss` shape the rest of the app uses (driver card /
 * detail). Used as `:formatter="timestampColumn"` on a column whose
 * {@code prop} already points at the ISO string. Blank cells fall
 * through as empty strings so the table does not display "Invalid Date".
 */
export const timestampColumn = (_row: unknown, _col: unknown, cellValue: unknown): string => {
  if (cellValue == null || cellValue === '') return '';
  return timestamp(String(cellValue));
};

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

/**
 * Calculate date difference
 *
 * @param date1 First date
 * @param date2 Second date
 * @returns {{leave1: number, hours: number, seconds: number, leave2: number, leave3: number, minutes: number, days: number}}
 */
interface DateDiff {
  leave1: number;
  leave2: number;
  leave3: number;
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
}

export const calcDate = (date1: Date, date2: Date): DateDiff => {
  const date3 = date2.getTime() - date1.getTime();

  const days = Math.floor(date3 / (24 * 3600 * 1000));

  const leave1 = date3 % (24 * 3600 * 1000);
  const hours = Math.floor(leave1 / (3600 * 1000));

  const leave2 = leave1 % (3600 * 1000);
  const minutes = Math.floor(leave2 / (60 * 1000));

  const leave3 = leave2 % (60 * 1000);
  const seconds = Math.round(date3 / 1000);
  return {
    leave1,
    leave2,
    leave3,
    days,
    hours,
    minutes,
    seconds,
  };
};
