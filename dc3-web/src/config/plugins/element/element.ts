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

import type {App} from 'vue';
import * as ElementIcons from '@element-plus/icons-vue';
// Per-component styles for Element Plus APIs imported manually outside
// templates (ElMessage / ElNotification / ElMessageBox / ElLoading) — template
// components get their styles injected by the unplugin-vue-components resolver.
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/notification/style/css';
import 'element-plus/es/components/message-box/style/css';
import 'element-plus/es/components/loading/style/css';
// Dark-mode variable overrides, activated by the .dark class on <html>
// (toggled by src/store/modules/app.ts). Standalone override layer — safe
// alongside the on-demand per-component style imports above.
import 'element-plus/theme-chalk/dark/css-vars.css';

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
