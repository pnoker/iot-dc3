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

import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import router from '@/config/router';
import { ElLoading } from 'element-plus';

import { cancelToken, generateSalt, generateToken } from '@/api/token';

import CommonConstant from '@/config/constant/common';
import type { Login } from '@/config/entity';
import { getStorage, removeStorage, setStorage } from '@/utils/StorageUtil';
import { isNull } from '@/utils/utils';
import { md5 } from 'js-md5';

export const useAuthStore = defineStore('auth', () => {
  // State
  const tenant = ref('default');
  const name = ref('dc3');

  // Getters
  const getTenant = computed(() => {
    return getStorage(CommonConstant.X_AUTH_TENANT);
  });

  const getName = computed(() => {
    return getStorage(CommonConstant.X_AUTH_LOGIN);
  });

  // Actions
  const setToken = (login: Login) => {
    setStorage(CommonConstant.X_AUTH_TENANT, login.tenant);
    setStorage(CommonConstant.X_AUTH_LOGIN, login.name);
    setStorage(CommonConstant.X_AUTH_TOKEN, { salt: login.salt, token: login.token });

    tenant.value = login.tenant || 'default';
    name.value = login.name || 'dc3';
  };

  const removeToken = () => {
    removeStorage(CommonConstant.X_AUTH_TENANT);
    removeStorage(CommonConstant.X_AUTH_LOGIN);
    removeStorage(CommonConstant.X_AUTH_TOKEN);
  };

  const login = async (form: any) => {
    const loading = ElLoading.service({
      lock: true,
      text: '登录中,请稍后...',
    });
    const loginData: Login = {
      tenant: form.tenant,
      name: form.name,
    };
    try {
      const saltRes = await generateSalt(loginData);
      const salt: string = saltRes.data;
      const loginWithPassword: Login = {
        tenant: form.tenant,
        name: form.name,
        salt: salt,
        password: md5.hex(md5.hex(form.password) + salt),
      };

      const tokenRes = await generateToken(loginWithPassword);
      setToken({
        tenant: loginWithPassword.tenant,
        name: loginWithPassword.name,
        salt: loginWithPassword.salt,
        token: tokenRes.data,
      });
      await router.push({ name: 'home' });
    } finally {
      loading.close();
    }
  };

  const logout = async () => {
    const tenantValue = getStorage(CommonConstant.X_AUTH_TENANT);
    const userValue = getStorage(CommonConstant.X_AUTH_LOGIN);
    if (!isNull(tenantValue) && !isNull(userValue)) {
      const loginData = {
        tenant: tenantValue,
        name: userValue,
      } as Login;
      await cancelToken(loginData);
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
    logout,
  };
});
