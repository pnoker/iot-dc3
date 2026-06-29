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

import {defineStore} from 'pinia';
import {computed, ref} from 'vue';
import router from '@/config/router';
import i18n from '@/config/i18n';
import {ElLoading} from 'element-plus';

import {cancelToken, changePassword, generateSalt, generateToken} from '@/api/token';

import {AUTH_HEADERS} from '@/config/constant/common';
import {PASSWORD_CHANGE_CODES} from '@/config/constant/axios';
import type {Login} from '@/config/types';
import {getStorage, removeStorage, setStorage} from '@/utils/storageUtil';
import {failMessage} from '@/utils/notificationUtil';
import {isNull} from '@/utils/validationUtil';

interface LoginForm {
  tenant: string;
  name: string;
  password: string;
}

export const useAuthStore = defineStore('auth', () => {
  // State
  const tenant = ref('default');
  const name = ref('dc3');

  // Getters
  const getTenant = computed(() => {
    return getStorage(AUTH_HEADERS.TENANT);
  });

  const getName = computed(() => {
    return getStorage(AUTH_HEADERS.LOGIN);
  });

  // Actions
  const setToken = (login: Login) => {
    setStorage(AUTH_HEADERS.TENANT, login.tenant);
    setStorage(AUTH_HEADERS.LOGIN, login.name);
    setStorage(AUTH_HEADERS.TOKEN, {salt: login.salt, token: login.token});

    tenant.value = login.tenant || 'default';
    name.value = login.name || 'dc3';
  };

  const removeToken = () => {
    removeStorage(AUTH_HEADERS.TENANT);
    removeStorage(AUTH_HEADERS.LOGIN);
    removeStorage(AUTH_HEADERS.TOKEN);
  };

  const login = async (form: LoginForm) => {
    const loading = ElLoading.service({
      lock: true,
      text: i18n.global.t('login.loading'),
    });
    try {
      const saltRes = await generateSalt({tenant: form.tenant, name: form.name});
      const salt: string = saltRes.data;
      if (!salt) {
        failMessage(i18n.global.t('login.failed'));
        return;
      }
      const loginWithPassword: Login = {
        tenant: form.tenant,
        name: form.name,
        salt: salt,
        password: form.password,
      };

      const tokenRes = await generateToken(loginWithPassword);
      setToken({
        tenant: loginWithPassword.tenant,
        name: loginWithPassword.name,
        salt: loginWithPassword.salt,
        token: tokenRes.data,
      });
      await router.push({name: 'home'});
    } catch (error) {
      const code = (error as {code?: string})?.code;
      if (code && PASSWORD_CHANGE_CODES.includes(code as never)) {
        // Password change required / expired: surface the code so the view can
        // open the change-password dialog instead of reporting a failed login.
        throw error;
      }
      failMessage(i18n.global.t('login.failed'));
    } finally {
      loading.close();
    }
  };

  const changeLoginPassword = async (form: LoginForm & {newPassword: string}) => {
    await changePassword({
      tenant: form.tenant,
      name: form.name,
      password: form.password,
      newPassword: form.newPassword,
    });
  };

  const logout = async () => {
    const tenantValue = getStorage(AUTH_HEADERS.TENANT);
    const userValue = getStorage(AUTH_HEADERS.LOGIN);
    if (!isNull(tenantValue) && !isNull(userValue)) {
      try {
        await cancelToken({tenant: tenantValue, name: userValue} as Login);
      } catch {
        // Server unreachable — proceed with local logout
      }
    }
    removeToken();
  };

  return {
    // State
    tenant,
    name,
    // Getters
    getTenant,
    getName,
    // Actions
    login,
    changeLoginPassword,
    logout,
  };
});
