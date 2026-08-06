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
import {AUTH_HEADERS} from '@/config/constant/common';
import {setStorage} from '@/utils/storageUtil';

import '@/styles/global.scss'; // config app

// Pre-seed auth in mock mode BEFORE the router is installed. Vue Router's
// initial navigation runs as a microtask, ahead of the async mock-chunk
// import in bootstrap() below; without this the guard sees empty storage and
// bounces every route to /login on first load.
if (import.meta.env.MODE === 'mock') {
  setStorage(AUTH_HEADERS.TENANT, 'default');
  setStorage(AUTH_HEADERS.LOGIN, 'dc3');
  setStorage(AUTH_HEADERS.TOKEN, {salt: 'mock-salt', token: 'mock-token'});
}

// config app
const app = createApp(App);
app.use(router);
app.use(createPinia());
app.use(i18n);
plugins(app);
app.config.errorHandler = (err, _instance, info) => {
  logger.error('Global error:', err, 'Info:', info);
};
/**
 * Static demo build: install a mock axios adapter before mounting so the app
 * runs on fake data with no backend. Vite replaces import.meta.env.MODE with a
 * build-time literal, so this branch (and the dynamically imported mock chunk)
 * is dead-code eliminated from production builds.
 */
async function bootstrap(): Promise<void> {
  if (import.meta.env.MODE === 'mock') {
    const {setupMock} = await import('@/mock');
    setupMock();
  }
  app.mount('#app');
}

void bootstrap();

export default app;
