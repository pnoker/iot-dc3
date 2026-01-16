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
import { Connection, Hide, Histogram, List, Management, Promotion, View } from '@element-plus/icons-vue';
import element from 'element-plus';
import 'element-plus/dist/index.css';
import './element-variables.scss';
import locale from 'element-plus/es/locale/lang/zh-cn';

/**
 * Element Plus icons to register globally
 */
const ICONS = [Hide, Histogram, List, Management, Promotion, View, Connection] as const;

/**
 * Element Plus plugin configuration
 * Registers Element Plus components and icons globally
 *
 * @param app Vue application instance
 */
export default function setupElementPlus(app: App): void {
  // Register icons globally
  ICONS.forEach((icon) => {
    if (icon.name) {
      app.component(icon.name, icon);
    }
  });

  // Install Element Plus with Chinese locale
  app.use(element, { locale });
}
