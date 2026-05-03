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
    class="things-dialog"
    draggable
    :title="reactiveData.mode === 'add' ? t('settings.resource.addTitle') : t('settings.resource.editTitle')"
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" label-position="top">
      <el-form-item :label="t('settings.resource.parentResourceId')" prop="parentResourceId">
        <el-tree-select
          v-model="reactiveData.form.parentResourceId"
          :data="parentTreeOptions"
          :props="{ label: 'resourceName', children: 'children' }"
          :placeholder="t('settings.resource.parentResourceIdPlaceholder')"
          clearable
          check-strictly
          node-key="id"
        />
      </el-form-item>
      <el-form-item :label="t('settings.resource.resourceName')" prop="resourceName">
        <el-input
          v-model="reactiveData.form.resourceName"
          clearable
          :placeholder="t('settings.resource.resourceNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('settings.resource.resourceCode')" prop="resourceCode">
        <el-input
          v-model="reactiveData.form.resourceCode"
          clearable
          :placeholder="t('settings.resource.resourceCodePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('settings.resource.resourceType')" prop="resourceTypeFlag">
        <el-select v-model="reactiveData.form.resourceTypeFlag" clearable>
          <el-option label="DRIVER" value="DRIVER" />
          <el-option label="PROFILE" value="PROFILE" />
          <el-option label="POINT" value="POINT" />
          <el-option label="DEVICE" value="DEVICE" />
          <el-option label="DATA" value="DATA" />
          <el-option label="MENU" value="MENU" />
          <el-option label="API" value="API" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.resource.entityId')" prop="entityId">
        <el-input
          v-model="reactiveData.form.entityId"
          clearable
          :placeholder="t('settings.resource.entityIdPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('common.enableFlag')" prop="enableFlag">
        <el-switch
          v-model="reactiveData.form.enableFlag"
          active-value="ENABLE"
          inactive-value="DISABLE"
          :active-text="t('common.enable')"
          :inactive-text="t('common.disable')"
        />
      </el-form-item>
      <el-form-item :label="t('common.remark')" prop="remark">
        <el-input v-model="reactiveData.form.remark" clearable maxlength="300" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <div class="things-dialog-footer">
      <el-button @click="reactiveData.visible = false">{{ t('common.cancel') }}</el-button>
      <el-button plain type="success" @click="reset">{{ t('common.reset') }}</el-button>
      <el-button type="primary" :loading="reactiveData.submitting" @click="submit">
        {{ t('common.confirm') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
  @use '@/styles/things-dialog.scss';
</style>
