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
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    :title="
      reactiveData.mode === 'add' ? t('settings.serviceAccount.addTitle') : t('settings.serviceAccount.editTitle')
    "
    class="things-dialog"
    draggable
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" label-position="top">
      <el-form-item :label="t('settings.serviceAccount.serviceAccountName')" prop="serviceAccountName">
        <el-input
          v-model="reactiveData.form.serviceAccountName"
          :placeholder="t('settings.serviceAccount.serviceAccountNamePlaceholder')"
          clearable
          maxlength="32"
          show-word-limit
        />
      </el-form-item>
      <el-form-item :label="t('settings.serviceAccount.ownerPrincipalId')">
        <el-input v-model="reactiveData.form.ownerPrincipalId" disabled />
      </el-form-item>
      <el-form-item :label="t('settings.serviceAccount.purpose')">
        <el-input
          v-model="reactiveData.form.purpose"
          :placeholder="t('settings.serviceAccount.purposePlaceholder')"
          type="textarea"
          maxlength="255"
          show-word-limit
        />
      </el-form-item>
      <el-form-item :label="t('settings.serviceAccount.expireTime')">
        <el-date-picker
          v-model="reactiveData.form.expireTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="reactiveData.form.enableFlag" />
      </el-form-item>
    </el-form>
    <div class="things-dialog-footer">
      <el-button @click="reactiveData.visible = false">{{ t('common.cancel') }}</el-button>
      <el-button plain @click="reset">{{ t('common.reset') }}</el-button>
      <el-button :loading="reactiveData.submitting" type="primary" @click="submit">
        {{ t('common.confirm') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts" src="./index.ts"></script>
