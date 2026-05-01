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
    class="things-dialog things-dialog--wide"
    draggable
    :title="reactiveData.mode === 'add' ? t('settings.menu.addTitle') : t('settings.menu.editTitle')"
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" label-position="top" class="things-form-grid">
      <el-form-item class="things-form-grid__span-2" :label="t('settings.menu.parentMenuId')" prop="parentMenuId">
        <el-tree-select
          v-model="reactiveData.form.parentMenuId"
          :data="parentTreeOptions"
          :props="{ label: 'menuName', children: 'children' }"
          :placeholder="t('settings.menu.parentMenuIdPlaceholder')"
          clearable
          check-strictly
          node-key="id"
        />
      </el-form-item>
      <el-form-item :label="t('settings.menu.menuName')" prop="menuName">
        <el-input
          v-model="reactiveData.form.menuName"
          clearable
          :placeholder="t('settings.menu.menuNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('settings.menu.menuCode')" prop="menuCode">
        <el-input
          v-model="reactiveData.form.menuCode"
          clearable
          :placeholder="t('settings.menu.menuCodePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('settings.menu.menuType')" prop="menuTypeFlag">
        <el-select v-model="reactiveData.form.menuTypeFlag">
          <el-option v-for="opt in MENU_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.menu.menuLevel')" prop="menuLevel">
        <el-select v-model="reactiveData.form.menuLevel">
          <el-option v-for="opt in MENU_LEVEL_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.menu.menuIndex')" prop="menuIndex">
        <el-input-number v-model="reactiveData.form.menuIndex" :min="0" :max="999" />
      </el-form-item>
      <el-form-item :label="t('settings.menu.menuIcon')" prop="icon">
        <el-input v-model="reactiveData.form.icon" clearable :placeholder="t('settings.menu.menuIconPlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('settings.menu.menuUrl')" prop="url">
        <el-input v-model="reactiveData.form.url" clearable :placeholder="t('settings.menu.menuUrlPlaceholder')" />
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
