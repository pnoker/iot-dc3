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
    :title="reactiveData.mode === 'add' ? t('settings.resource.addTitle') : t('settings.resource.editTitle')"
    width="560px"
    :close-on-click-modal="false"
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" label-width="100px">
      <el-form-item :label="t('settings.resource.parentResourceId')" prop="parentResourceId">
        <el-input
          v-model="reactiveData.form.parentResourceId"
          :placeholder="t('settings.resource.parentResourceIdPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('settings.resource.resourceName')" prop="resourceName">
        <el-input
          v-model="reactiveData.form.resourceName"
          :placeholder="t('settings.resource.resourceNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('settings.resource.resourceCode')" prop="resourceCode">
        <el-input
          v-model="reactiveData.form.resourceCode"
          :placeholder="t('settings.resource.resourceCodePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('settings.resource.resourceType')" prop="resourceTypeFlag">
        <el-select v-model="reactiveData.form.resourceTypeFlag" clearable>
          <el-option label="MENU" value="MENU" />
          <el-option label="API" value="API" />
          <el-option label="BUTTON" value="BUTTON" />
          <el-option label="DATA" value="DATA" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.resource.entityId')" prop="entityId">
        <el-input v-model="reactiveData.form.entityId" :placeholder="t('settings.resource.entityIdPlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('common.enable')" prop="enableFlag">
        <el-switch
          v-model="reactiveData.form.enableFlag"
          active-value="ENABLE"
          inactive-value="DISABLE"
          :active-text="t('common.enable')"
          :inactive-text="t('common.disable')"
        />
      </el-form-item>
      <el-form-item :label="t('common.remark')" prop="remark">
        <el-input v-model="reactiveData.form.remark" type="textarea" :rows="3" />
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
