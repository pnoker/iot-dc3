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
  <el-dialog
    v-model="reactiveData.visible"
    :title="reactiveData.mode === 'add' ? t('settings.user.addTitle') : t('settings.user.editTitle')"
    width="560px"
    :close-on-click-modal="false"
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" label-width="90px">
      <el-form-item :label="t('settings.user.userName')" prop="userName">
        <el-input
          v-model="reactiveData.form.userName"
          :placeholder="t('settings.user.userNamePlaceholder')"
          :disabled="reactiveData.mode === 'edit'"
        />
      </el-form-item>
      <el-form-item :label="t('settings.user.nickName')" prop="nickName">
        <el-input v-model="reactiveData.form.nickName" :placeholder="t('settings.user.nickNamePlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('settings.user.phone')" prop="phone">
        <el-input v-model="reactiveData.form.phone" :placeholder="t('settings.user.phonePlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('settings.user.email')" prop="email">
        <el-input v-model="reactiveData.form.email" :placeholder="t('settings.user.emailPlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('common.enable')" prop="enableFlag">
        <el-switch
          v-model="reactiveData.form.enableFlag"
          :active-value="0"
          :inactive-value="1"
          :active-text="t('common.enable')"
          :inactive-text="t('common.disable')"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reactiveData.visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="reactiveData.submitting" @click="submit">
        {{ t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" src="./index.ts"></script>
