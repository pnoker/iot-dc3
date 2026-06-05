<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <div class="login-container">
    <div class="login-wrapper-left animated bounce-in-down">
      <div class="login-left">
        <img class="img" src="/images/logo/logo-white.png" />
      </div>
    </div>
    <Particles />
    <div class="login-wrapper-right animated bounce-in-down">
      <div class="login-right">
        <div class="login-main">
          <h4 class="login-title">{{ t('login.title') }}</h4>
          <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule" class="login-form" status-icon>
            <el-form-item prop="tenant">
              <el-input
                v-model="reactiveData.formData.tenant"
                :placeholder="t('login.tenantPlaceholder')"
                :prefix-icon="Box"
                auto-complete="off"
                @keyup.enter="handleLogin"
              >
              </el-input>
            </el-form-item>
            <el-form-item prop="name">
              <el-input
                v-model="reactiveData.formData.name"
                :placeholder="t('login.usernamePlaceholder')"
                :prefix-icon="User"
                auto-complete="off"
                @keyup.enter="handleLogin"
              ></el-input>
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="reactiveData.formData.password"
                :placeholder="t('login.passwordPlaceholder')"
                :prefix-icon="Lock"
                :type="reactiveData.passwordType"
                auto-complete="off"
                @keyup.enter="handleLogin"
              >
                <template #append>
                  <el-button :icon="reactiveData.isHide" @click="showPassword" />
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button class="login-submit" type="primary" @click.prevent="handleLogin">{{
                t('login.submit')
              }}</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { reactive, ref, unref } from 'vue';
  import type { FormInstance, FormRules } from 'element-plus';
  import { Box, Lock, User } from '@element-plus/icons-vue';
  import { useI18n } from 'vue-i18n';

  import { useAuthStore } from '@/store';

  import Particles from '@/components/particles/Particles.vue';

  interface LoginFormModel {
    tenant: string;
    name: string;
    password: string;
  }

  interface LoginViewState {
    isHide: 'View' | 'Hide';
    passwordType: '' | 'password';
    formData: LoginFormModel;
  }

  const { t } = useI18n();
  const authStore = useAuthStore();
  // 定义表单引用
  const formDataRef = ref<FormInstance>();

  // 定义响应式数据
  const tenant = typeof authStore.getTenant === 'string' ? authStore.getTenant : 'default';
  const name = typeof authStore.getName === 'string' ? authStore.getName : 'dc3';
  const reactiveData = reactive<LoginViewState>({
    isHide: 'View',
    passwordType: 'password',
    formData: {
      tenant,
      name,
      password: '',
    },
  });

  // 定义表单校验规则
  const formRule = reactive<FormRules>({
    tenant: [{ required: true, message: t('login.tenantRequired'), trigger: 'blur' }],
    name: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
    password: [
      { required: true, message: t('login.passwordRequired'), trigger: 'blur' },
      { min: 6, message: t('login.passwordMin'), trigger: 'blur' },
    ],
  });

  // 显示, 隐藏密码
  const showPassword = () => {
    reactiveData.passwordType === '' ? (reactiveData.passwordType = 'password') : (reactiveData.passwordType = '');
    reactiveData.isHide === 'View' ? (reactiveData.isHide = 'Hide') : (reactiveData.isHide = 'View');
  };

  // 登录
  const handleLogin = async () => {
    const form = unref(formDataRef);
    if (!form) {
      return;
    }

    try {
      await form.validate();
      await authStore.login(reactiveData.formData);
    } catch {
      // validation errors are displayed by Element Plus
    }
  };
</script>

<style lang="scss" scoped>
  @use '@/views/login/style.scss';
</style>
