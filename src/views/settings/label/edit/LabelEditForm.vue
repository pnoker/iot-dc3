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
    :title="reactiveData.mode === 'add' ? t('settings.label.addTitle') : t('settings.label.editTitle')"
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" label-position="top" class="things-form-grid">
      <el-form-item :label="t('settings.common.entityType')" prop="entityTypeFlag">
        <el-select v-model="reactiveData.form.entityTypeFlag" clearable>
          <el-option v-for="opt in ENTITY_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.label.labelColor')" prop="labelColor">
        <el-color-picker v-model="reactiveData.form.labelColor" show-alpha />
      </el-form-item>
      <el-form-item :label="t('settings.label.labelName')" prop="labelName">
        <el-input
          v-model="reactiveData.form.labelName"
          clearable
          :placeholder="t('settings.label.labelNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('settings.label.labelCode')" prop="labelCode">
        <el-input
          v-model="reactiveData.form.labelCode"
          clearable
          :placeholder="t('settings.label.labelCodePlaceholder')"
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
      <el-form-item class="things-form-grid__span-2" :label="t('common.remark')" prop="remark">
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
