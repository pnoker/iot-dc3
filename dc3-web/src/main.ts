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

import {createPinia} from 'pinia';
import App from '@/App.vue';
import i18n from '@/config/i18n';
import plugins from '@/config/plugins/index';
import router from '@/config/router';
import {createApp} from 'vue';

import '@/styles/global.scss'; // config app

// config app
const app = createApp(App);
app.use(router);
app.use(createPinia());
app.use(i18n);
plugins(app);
app.config.errorHandler = (err, instance, info) => {
  console.error('Global error:', err, 'Component:', instance, 'Info:', info);
};
app.mount('#app');

export default app;
