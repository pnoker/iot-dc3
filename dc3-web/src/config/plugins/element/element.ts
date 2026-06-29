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

import type {App} from 'vue';
import * as ElementIcons from '@element-plus/icons-vue';
import 'element-plus/dist/index.css';

/**
 * Registers every icon exported by `@element-plus/icons-vue` globally so
 * templates can use the icon class name as a string — both legacy call
 * sites (`<component :is="'Promotion'">`) and the backend-driven menu, where
 * `menu_ext.content.icon` stores the icon name, work without any per-icon
 * allow-list. Adding a new icon to a seeded menu requires no code change
 * here.
 */
export default function setupElementPlus(app: App): void {
  for (const [name, comp] of Object.entries(ElementIcons)) {
    // Filter to capitalized component exports (skip `__esModule`, etc.).
    if (!/^[A-Z]/.test(name) || !comp || typeof comp !== 'object') continue;
    app.component(name, comp as any);
  }
}
