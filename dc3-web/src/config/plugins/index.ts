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
