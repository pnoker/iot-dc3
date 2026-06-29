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

import router from '@/config/router';
import {useAuthStore} from '@/store';
import {setCopyContent} from '@/utils/clipboardUtil';

/**
 * Copy content to clipboard with success message
 *
 * @param content Content to copy
 * @param message Success message to display
 */
export const copy = (content: string, message: string) => {
  setCopyContent(content, true, message);
};

/**
 * Logout and redirect to login page
 */
export const logout = async () => {
  const authStore = useAuthStore();
  await authStore.logout();
  await router.push({name: 'login'});
};
