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
 * Format date to string
 *
 * @param date Date object or string
 * @returns {string} Formatted date string
 */
export function dateFormat(date: any): string {
  let format = 'yyyy-MM-dd hh:mm:ss';
  if (date !== 'Invalid Date') {
    const o: Record<string, any> = {
      'M+': date.getMonth() + 1,
      'd+': date.getDate(),
      'h+': date.getHours(),
      'm+': date.getMinutes(),
      's+': date.getSeconds(),
      'q+': Math.floor((date.getMonth() + 3) / 3),
      S: date.getMilliseconds(),
    };
    if (/(y+)/.test(format)) format = format.replace(RegExp.$1, (date.getFullYear() + '').substr(4 - RegExp.$1.length));
    for (const k in o)
      if (new RegExp('(' + k + ')').test(format))
        format = format.replace(RegExp.$1, RegExp.$1.length === 1 ? o[k] : ('00' + o[k]).substr(String(o[k]).length));
    return format;
  }
  return '';
}

/**
 * Calculate date difference
 *
 * @param date1 First date
 * @param date2 Second date
 * @returns {{leave1: number, hours: number, seconds: number, leave2: number, leave3: number, minutes: number, days: number}}
 */
export const calcDate = (date1: any, date2: any): any => {
  const date3 = date2 - date1;

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
    days: days,
    hours: hours,
    minutes: minutes,
    seconds: seconds,
  };
};
