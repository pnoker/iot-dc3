<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
        <el-input :model-value="ownerName" disabled/>
      </el-form-item>
      <el-form-item :label="t('settings.serviceAccount.purpose')">
        <el-input
          v-model="reactiveData.form.purpose"
          :placeholder="t('settings.serviceAccount.purposePlaceholder')"
          maxlength="255"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
      <el-form-item :label="t('settings.serviceAccount.expireTime')">
        <el-date-picker
          v-model="reactiveData.form.expireTime"
          style="width: 100%"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
        />
      </el-form-item>
      <el-form-item :label="t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="reactiveData.form.enableFlag"/>
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
