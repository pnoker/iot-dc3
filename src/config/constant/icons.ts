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

import {
  Bell,
  Box,
  Calendar,
  Cpu,
  DataAnalysis,
  Discount,
  Connection,
  Expand,
  Folder,
  Goods,
  HomeFilled,
  House,
  Key,
  Link,
  List,
  Lock,
  Management,
  Menu,
  Monitor,
  Odometer,
  Operation,
  PieChart,
  Platform,
  Promotion,
  Setting,
  Share,
  Stopwatch,
  Tickets,
  Tools,
  TrendCharts,
  User,
  UserFilled,
  WalletFilled,
} from '@element-plus/icons-vue';
import type { Component } from 'vue';

/**
 * Name → component lookup used to render server-driven menu icons.
 * Add additional element-plus icons here as new menus reference them.
 */
export const iconMap: Record<string, Component> = {
  Bell,
  Box,
  Calendar,
  Connection,
  Cpu,
  DataAnalysis,
  Discount,
  Expand,
  Folder,
  Goods,
  HomeFilled,
  House,
  Key,
  Link,
  List,
  Lock,
  Management,
  Menu,
  Monitor,
  Odometer,
  Operation,
  PieChart,
  Platform,
  Promotion,
  Setting,
  Share,
  Stopwatch,
  Tickets,
  Tools,
  TrendCharts,
  User,
  UserFilled,
  WalletFilled,
};

/**
 * Ordered list of icon names — used by menu-edit pickers to show a stable
 * order (object key order isn't contractually stable across engines).
 */
export const iconNames: string[] = Object.keys(iconMap).sort((a, b) => a.localeCompare(b));

export const resolveIcon = (name?: string): Component | undefined => {
  if (!name) return undefined;
  return iconMap[name];
};
