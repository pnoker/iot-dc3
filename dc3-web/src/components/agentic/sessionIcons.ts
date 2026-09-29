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

import {
  Connection,
  Lightning,
  Monitor,
  Odometer,
  Operation,
  Tools,
  TrendCharts,
  WarningFilled,
} from '@element-plus/icons-vue';

import type {AgenticSessionExt} from '@/config/types';

const sessionIcons = {
  monitor: Monitor,
  warning: WarningFilled,
  trend: TrendCharts,
  connection: Connection,
  odometer: Odometer,
  tools: Tools,
  operation: Operation,
  lightning: Lightning,
} as const;

/**
 * Resolves a session category to its rail/dropdown icon.
 *
 * @param icon session category from sessionExt
 * @returns the matching icon component
 */
export const sessionIcon = (icon?: AgenticSessionExt['icon']) => sessionIcons[icon || 'monitor'] || Monitor;
