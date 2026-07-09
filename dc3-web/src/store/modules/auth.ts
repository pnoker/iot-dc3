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
      const code = (error as { code?: string })?.code;
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

  const changeLoginPassword = async (form: LoginForm & { newPassword: string }) => {
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
