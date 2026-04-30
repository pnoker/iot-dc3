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
    :title="t('settings.user.assignRolesTitle')"
    width="720px"
    :close-on-click-modal="false"
  >
    <div v-loading="reactiveData.loading" class="assign-body">
      <div class="assign-target">
        <span class="assign-label">{{ t('settings.user.userName') }}:</span>
        <span class="assign-value">{{ reactiveData.user.userName }}</span>
        <span class="assign-sep">/</span>
        <span class="assign-value">{{ reactiveData.user.nickName }}</span>
      </div>
      <el-transfer
        v-model="reactiveData.selectedIds"
        :data="reactiveData.allRoles"
        :titles="[t('settings.user.rolesAll'), t('settings.user.rolesOfUser')]"
        :props="{ key: 'id', label: 'label' }"
        filterable
      />
    </div>
    <template #footer>
      <el-button @click="reactiveData.visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="reactiveData.submitting" @click="submit">
        {{ t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
  .assign-body {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .assign-target {
    font-size: 14px;
    color: var(--el-text-color-regular);
  }
  .assign-label {
    color: var(--el-text-color-secondary);
    margin-right: 4px;
  }
  .assign-value {
    font-weight: 600;
  }
  .assign-sep {
    margin: 0 6px;
    color: var(--el-text-color-secondary);
  }
</style>
