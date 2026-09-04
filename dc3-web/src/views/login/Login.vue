<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

<template>
  <main class="login-container">
    <LoginAtmosphere/>
    <div class="login-orb login-orb-primary" aria-hidden="true"/>
    <div class="login-orb login-orb-accent" aria-hidden="true"/>

    <section class="login-shell">
      <app-preferences class="login-preferences" surface />

      <div class="login-story">
        <brand-lockup class="login-brand-pill" />

        <div class="login-story-copy">
          <span class="login-kicker">PHYSICAL AI RUNTIME</span>
          <h1>{{ t('login.heroTitle') }}</h1>
          <p>{{ t('login.heroDescription') }}</p>
          <div class="login-capabilities" :aria-label="t('login.capabilities')">
            <span>{{ t('login.capabilityConnect') }}</span>
            <span>{{ t('login.capabilityGovern') }}</span>
            <span>{{ t('login.capabilityAct') }}</span>
          </div>
        </div>

        <div class="login-runtime-flow" aria-hidden="true">
          <span>AI Agent</span>
          <i>→</i>
          <strong>IoT DC3</strong>
          <i>→</i>
          <span>{{ t('login.physicalWorld') }}</span>
        </div>
      </div>

      <div class="login-card-wrap">
        <div class="login-card">
          <brand-lockup class="login-card-brand" />
          <div class="login-main">
            <span class="login-card-kicker">{{ t('login.console') }}</span>
            <h2 class="login-title">{{ t('login.welcome') }}</h2>
            <p class="login-subtitle">{{ t('login.subtitle') }}</p>
            <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule" class="login-form" status-icon>
              <el-form-item prop="tenant">
                <el-input
                  v-model="reactiveData.formData.tenant"
                  :placeholder="t('login.tenantPlaceholder')"
                  :prefix-icon="Box"
                  auto-complete="off"
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              <el-form-item prop="name">
                <el-input
                  v-model="reactiveData.formData.name"
                  :placeholder="t('login.usernamePlaceholder')"
                  :prefix-icon="User"
                  auto-complete="off"
                  @keyup.enter="handleLogin"
                />
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
                    <el-button
                      :aria-label="t('login.togglePassword')"
                      :icon="reactiveData.isHide"
                      @click="showPassword"
                    />
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-button :loading="loading" class="login-submit" type="primary" @click.prevent="handleLogin">
                  {{ t('login.submit') }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </section>

    <footer class="login-footer">
      <span class="login-footer-main">
        <a class="login-footer-brand" href="https://dc3.site" rel="noopener" target="_blank">IoT DC3</a>
        <span aria-hidden="true">&nbsp;·&nbsp;</span>
        <span class="login-footer-tagline">{{ t('login.tagline') }}</span>
      </span>
      <span class="login-footer-legal">{{ copyright }}</span>
    </footer>

    <el-dialog v-model="changePasswordVisible" :title="t('login.changePasswordTitle')" width="420px">
      <el-alert :closable="false" :title="changePasswordHint" class="mb-4" show-icon type="warning"/>
      <el-form ref="changePasswordRef" :model="changePasswordData" :rules="changePasswordRule" label-width="0">
        <el-form-item prop="newPassword">
          <el-input
            v-model="changePasswordData.newPassword"
            :placeholder="t('login.newPasswordPlaceholder')"
            show-password
            type="password"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="changePasswordData.confirmPassword"
            :placeholder="t('login.confirmPasswordPlaceholder')"
            show-password
            type="password"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :loading="changePasswordLoading" type="primary" @click="handleChangePassword">
          {{ t('login.changePasswordSubmit') }}
        </el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script lang="ts" setup>
import {computed, reactive, ref, unref} from 'vue';
import type {FormInstance, FormRules} from 'element-plus';
import {Box, Lock, User} from '@element-plus/icons-vue';
import {useI18n} from 'vue-i18n';

import {useAuthStore} from '@/store';
import {PASSWORD_CHANGE_CODES} from '@/config/constant/axios';
import {failMessage, successMessage} from '@/utils/notificationUtil';

import AppPreferences from '@/components/layout/AppPreferences.vue';
import BrandLockup from '@/components/brand/BrandLockup.vue';
import LoginAtmosphere from '@/components/login-atmosphere/LoginAtmosphere.vue';

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

const {t} = useI18n();
const authStore = useAuthStore();
const formDataRef = ref<FormInstance>();
const loading = ref(false);
// The range end tracks the current year at render time (2016–2026 today,
// 2016–2027 next year), matching the family sites' footer contract.
const copyright = computed(() => `AGPL-3.0 · © 2016–${new Date().getFullYear()}`);

const tenant = typeof authStore.getTenant === 'string' ? authStore.getTenant : 'default';
const name = typeof authStore.getName === 'string' ? authStore.getName : 'dc3';
// The mock build is a public, backend-free demo. Prefill its seeded password so
// visitors can enter directly, while every real backend mode still starts empty.
const password = import.meta.env.MODE === 'mock' ? 'dc3dc3dc3' : '';
const reactiveData = reactive<LoginViewState>({
  isHide: 'View',
  passwordType: 'password',
  formData: {
    tenant,
    name,
    password,
  },
});

const formRule = computed<FormRules>(() => ({
  tenant: [{required: true, message: t('login.tenantRequired'), trigger: 'blur'}],
  name: [{required: true, message: t('login.usernameRequired'), trigger: 'blur'}],
  password: [
    {required: true, message: t('login.passwordRequired'), trigger: 'blur'},
    {min: 6, message: t('login.passwordMin'), trigger: 'blur'},
  ],
}));

const showPassword = () => {
  reactiveData.passwordType === '' ? (reactiveData.passwordType = 'password') : (reactiveData.passwordType = '');
  reactiveData.isHide === 'View' ? (reactiveData.isHide = 'Hide') : (reactiveData.isHide = 'View');
};

const handleLogin = async () => {
  const form = unref(formDataRef);
  if (!form) {
    return;
  }

  try {
    await form.validate();
  } catch {
    // validation errors are displayed by Element Plus
    return;
  }

  loading.value = true;
  try {
    await authStore.login(reactiveData.formData);
  } catch (error) {
    const code = (error as { code?: string })?.code;
    if (code && (PASSWORD_CHANGE_CODES as readonly string[]).includes(code)) {
      openChangePassword(code);
    }
    // other failures already surface failMessage inside authStore.login
  } finally {
    loading.value = false;
  }
};

// Password change flow (mandatory change / expired password)
const changePasswordVisible = ref(false);
const changePasswordLoading = ref(false);
const changePasswordCode = ref('');
const changePasswordRef = ref<FormInstance>();
const changePasswordData = reactive({newPassword: '', confirmPassword: ''});

const changePasswordHint = computed(() =>
  changePasswordCode.value === 'R4032' ? t('login.passwordExpired') : t('login.changePasswordRequired')
);

const changePasswordRule = computed<FormRules>(() => ({
  newPassword: [
    {required: true, message: t('login.passwordRequired'), trigger: 'blur'},
    {min: 6, message: t('login.passwordMin'), trigger: 'blur'},
  ],
  confirmPassword: [
    {
      validator: (_rule, value, callback) =>
        value === changePasswordData.newPassword
          ? callback()
          : callback(new Error(t('login.confirmPasswordMismatch'))),
      trigger: 'blur',
    },
  ],
}));

const openChangePassword = (code: string) => {
  changePasswordCode.value = code;
  changePasswordData.newPassword = '';
  changePasswordData.confirmPassword = '';
  changePasswordVisible.value = true;
};

const handleChangePassword = async () => {
  const form = unref(changePasswordRef);
  if (!form) {
    return;
  }
  try {
    await form.validate();
  } catch {
    return;
  }
  changePasswordLoading.value = true;
  try {
    await authStore.changeLoginPassword({
      tenant: reactiveData.formData.tenant,
      name: reactiveData.formData.name,
      password: reactiveData.formData.password,
      newPassword: changePasswordData.newPassword,
    });
    changePasswordVisible.value = false;
    successMessage(t('login.changePasswordSuccess'));
    reactiveData.formData.password = changePasswordData.newPassword;
    await handleLogin();
  } catch {
    failMessage(t('login.changePasswordFailed'));
  } finally {
    changePasswordLoading.value = false;
  }
};
</script>

<style lang="scss" scoped>
@use '@/views/login/style.scss';
</style>
