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
    :title="reactiveData.mode === 'add' ? t('settings.group.addTitle') : t('settings.group.editTitle')"
    class="things-dialog"
    draggable
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" class="things-form-grid" label-position="top">
      <el-form-item :label="t('settings.common.entityType')" prop="groupTypeFlag">
        <el-select v-model="reactiveData.form.groupTypeFlag" clearable>
          <el-option v-for="opt in ENTITY_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.group.parentGroupId')" prop="parentGroupId">
        <el-tree-select
          v-model="reactiveData.form.parentGroupId"
          :data="parentTreeOptions"
          :placeholder="t('settings.group.parentGroupIdPlaceholder')"
          :props="{ label: 'groupName', children: 'children' }"
          check-strictly
          clearable
          node-key="id"
        />
      </el-form-item>
      <el-form-item :label="t('settings.group.groupName')" prop="groupName">
        <el-input
          v-model="reactiveData.form.groupName"
          :placeholder="t('settings.group.groupNamePlaceholder')"
          clearable
        />
      </el-form-item>
      <el-form-item :label="t('settings.group.groupCode')" prop="groupCode">
        <el-input
          v-model="reactiveData.form.groupCode"
          :placeholder="t('settings.group.groupCodePlaceholder')"
          clearable
        />
      </el-form-item>
      <el-form-item :label="t('settings.group.groupIndex')" prop="groupIndex">
        <el-input-number v-model="reactiveData.form.groupIndex" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item :label="t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="reactiveData.form.enableFlag" />
      </el-form-item>
      <el-form-item :label="t('common.remark')" class="things-form-grid__span-2" prop="remark">
        <el-input v-model="reactiveData.form.remark" clearable maxlength="300" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <div class="things-dialog-footer">
      <el-button @click="reactiveData.visible = false">{{ t('common.cancel') }}</el-button>
      <el-button plain type="success" @click="reset">{{ t('common.reset') }}</el-button>
      <el-button :loading="reactiveData.submitting" type="primary" @click="submit">
        {{ t('common.confirm') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
  @use '@/styles/things-dialog.scss';
</style>
