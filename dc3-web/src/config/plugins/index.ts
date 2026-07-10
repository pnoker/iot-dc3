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
import setupElementPlus from '@/config/plugins/element/element';
import setupHighlight from '@/config/plugins/highlight/highlight';

/**
 * IoT DC3 Platform ASCII art banner
 */
const PLATFORM_BANNER = `
.___     ___________ ________  _________ ________
|   | ___\\\\__    ___/ \\\\______ \\\\ \\\\   ___ \\\\\\\\_____  \\\\
|   |/  _ \\\\|    |     |    |  \\\\/    \\\\  \\\\/  _(__  <
|   (  <_> )    |     |    \`   \\\\     \\\\____/       \\\\
|___|\\\\____/|____|    /_______  /\\\\______  /______  /
                             \\\\/        \\\\/       \\\\/
https://doc.dc3.site
IoT DC3 Platform V2026.5.22`;

/**
 * Plugin setup function
 * Registers all application plugins (Element Plus, Highlight.js)
 *
 * @param app Vue application instance
 */
export default function setupPlugins(app: App): void {
  setupElementPlus(app);
  setupHighlight(app);

  console.log(PLATFORM_BANNER);
}
