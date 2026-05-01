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

import type { App } from 'vue';
import {
  Avatar,
  Connection,
  Cpu,
  DataAnalysis,
  DataLine,
  Document,
  Hide,
  Histogram,
  HomeFilled,
  House,
  Key,
  Link,
  List,
  Management,
  Menu as MenuIcon,
  Monitor,
  Promotion,
  Setting,
  Share,
  Tickets,
  User,
  UserFilled,
  View,
} from '@element-plus/icons-vue';
import 'element-plus/dist/index.css';
import './element-variables.scss';

// Global icon registry — referenced both by legacy templates that pass an icon
// class name string (`:is="'Promotion'"`) and by the backend-driven menu where
// menu_ext.content.icon stores the icon class name.
const ICONS = [
  Hide,
  Histogram,
  List,
  Management,
  Promotion,
  View,
  Connection,
  House,
  HomeFilled,
  Cpu,
  Tickets,
  Monitor,
  DataAnalysis,
  DataLine,
  Document,
  Setting,
  Share,
  User,
  UserFilled,
  Avatar,
  Key,
  Link,
  MenuIcon,
] as const;

export default function setupElementPlus(app: App): void {
  ICONS.forEach((icon) => {
    if (icon.name) {
      app.component(icon.name, icon);
    }
  });
}
