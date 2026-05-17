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
    :title="reactiveData.mode === 'add' ? t('settings.menu.addTitle') : t('settings.menu.editTitle')"
    class="things-dialog things-dialog--wide"
    draggable
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" class="things-form-grid" label-position="top">
      <el-form-item :label="t('settings.menu.parentMenuId')" class="things-form-grid__span-2" prop="parentMenuId">
        <el-tree-select
          v-model="reactiveData.form.parentMenuId"
          :data="parentTreeOptions"
          :placeholder="t('settings.menu.parentMenuIdPlaceholder')"
          :props="{ label: 'menuName', children: 'children' }"
          check-strictly
          clearable
          node-key="id"
        />
      </el-form-item>
      <el-form-item :label="t('settings.menu.menuName')" prop="menuName">
        <el-input
          v-model="reactiveData.form.menuName"
          :placeholder="t('settings.menu.menuNamePlaceholder')"
          clearable
        />
      </el-form-item>
      <el-form-item :label="t('settings.menu.menuCode')" prop="menuCode">
        <el-input
          v-model="reactiveData.form.menuCode"
          :placeholder="t('settings.menu.menuCodePlaceholder')"
          clearable
        />
      </el-form-item>
      <el-form-item :label="t('settings.menu.titleZh')" prop="titleZh">
        <el-input v-model="reactiveData.form.titleZh" :placeholder="t('settings.menu.titleZhPlaceholder')" clearable />
      </el-form-item>
      <el-form-item :label="t('settings.menu.titleEn')" prop="titleEn">
        <el-input v-model="reactiveData.form.titleEn" :placeholder="t('settings.menu.titleEnPlaceholder')" clearable />
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
        <el-input-number v-model="reactiveData.form.menuIndex" :max="999" :min="0" />
      </el-form-item>
      <el-form-item :label="t('settings.menu.menuIcon')" prop="icon">
        <el-select
          v-model="reactiveData.form.icon"
          :placeholder="t('settings.menu.menuIconPlaceholder')"
          clearable
          filterable
        >
          <template #prefix>
            <el-icon v-if="reactiveData.form.icon" :size="16">
              <component :is="resolveIcon(reactiveData.form.icon)" />
            </el-icon>
          </template>
          <el-option v-for="name in iconNames" :key="name" :label="name" :value="name">
            <span class="icon-option">
              <el-icon :size="16"><component :is="iconMap[name]" /></el-icon>
              <span>{{ name }}</span>
            </span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item :label="t('settings.menu.menuUrl')" prop="url">
        <el-input v-model="reactiveData.form.url" :placeholder="t('settings.menu.menuUrlPlaceholder')" clearable />
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

  .icon-option {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }
</style>
