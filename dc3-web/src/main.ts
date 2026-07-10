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

import {createPinia} from 'pinia';
import App from '@/App.vue';
import i18n from '@/config/i18n';
import plugins from '@/config/plugins/index';
import router from '@/config/router';
import {createApp} from 'vue';
import {logger} from '@/utils/log';

import '@/styles/global.scss'; // config app

// config app
const app = createApp(App);
app.use(router);
app.use(createPinia());
app.use(i18n);
plugins(app);
app.config.errorHandler = (err, _instance, info) => {
  logger.error('Global error:', err, 'Info:', info);
};
app.mount('#app');

export default app;
