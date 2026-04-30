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
    :title="reactiveData.mode === 'add' ? t('settings.role.addTitle') : t('settings.role.editTitle')"
    width="560px"
    :close-on-click-modal="false"
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" label-width="90px">
      <el-form-item :label="t('settings.role.parentRoleId')" prop="parentRoleId">
        <el-input v-model="reactiveData.form.parentRoleId" :placeholder="t('settings.role.parentRoleIdPlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('settings.role.roleName')" prop="roleName">
        <el-input v-model="reactiveData.form.roleName" :placeholder="t('settings.role.roleNamePlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('settings.role.roleCode')" prop="roleCode">
        <el-input v-model="reactiveData.form.roleCode" :placeholder="t('settings.role.roleCodePlaceholder')" />
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
