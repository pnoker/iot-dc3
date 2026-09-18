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
    v-model="visible"
    :append-to-body="true"
    :before-close="requestClose"
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    :title="isEdit ? $t('settings.agentic.editProvider') : $t('settings.agentic.addProvider')"
    class="things-dialog"
    destroy-on-close
    draggable
    @closed="onClosed"
  >
    <el-alert
      v-if="saveError"
      :closable="false"
      :title="$t('common.saveFailed')"
      class="things-dialog-form-alert"
      show-icon
      type="error"
    />
    <el-form ref="formRef" v-loading="submitting" :aria-busy="submitting" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="$t('settings.agentic.providerName')" prop="name">
        <el-input
          v-model="form.name"
          :disabled="submitting"
          :placeholder="$t('settings.agentic.providerNamePlaceholder')"
          clearable
          maxlength="128"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.providerType')" prop="providerType">
        <el-select
          v-model="form.providerType"
          :disabled="submitting"
          :placeholder="$t('settings.agentic.providerTypePlaceholder')"
          style="width: 100%"
        >
          <el-option v-for="pt in providerTypes" :key="pt.value" :label="pt.label" :value="pt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.baseUrl')" prop="baseUrl">
        <el-input
          v-model="form.baseUrl"
          :disabled="submitting"
          :placeholder="$t('settings.agentic.baseUrlPlaceholder')"
          clearable
          maxlength="256"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.apiKey')" prop="apiKey">
        <el-input
          v-model="form.apiKey"
          :disabled="submitting"
          :placeholder="$t('settings.agentic.apiKeyPlaceholder')"
          clearable
          maxlength="256"
          show-password
          type="password"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.default')">
        <el-switch
          v-model="form.defaultFlag"
          :disabled="submitting"
          :active-text="$t('common.yes')"
          :inactive-text="$t('common.no')"
          active-value="DEFAULT"
          inactive-value="NOT_DEFAULT"
        />
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')">
        <enable-flag-segmented v-model="form.enableFlag" :disabled="submitting"/>
      </el-form-item>
      <el-form-item :label="$t('common.remark')" prop="remark">
        <el-input v-model="form.remark" :disabled="submitting" :rows="3" maxlength="300" show-word-limit type="textarea"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="things-dialog-footer">
        <el-button :disabled="submitting" @click="requestClose()">{{ $t('common.cancel') }}</el-button>
        <el-button :disabled="submitting" plain @click="onReset">{{ $t('common.reset') }}</el-button>
        <el-button :loading="submitting" type="primary" @click="onSubmit">
          {{ $t('common.confirm') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, reactive, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import type {FormInstance, FormRules} from 'element-plus';
import {ElMessageBox} from 'element-plus';

import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import type {AgenticProvider} from '@/config/types';
import {remarkRules} from '@/utils/formRuleUtil';
import {enableFlagValue} from '@/utils/thingModelFormatUtil';

import {AGENTIC_PROVIDER_TYPES} from '../providerTypes';

const providerTypes = AGENTIC_PROVIDER_TYPES;

const emit = defineEmits<{
  (e: 'save', form: AgenticProvider & { apiKey?: string }, done: (close?: boolean) => void): void;
}>();

const visible = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
const saveError = ref(false);
const formRef = ref<FormInstance>();
const {t} = useI18n();

const initialForm = (): AgenticProvider & { apiKey?: string } => ({
  name: '',
  providerType: 'OPENAI_COMPATIBLE',
  baseUrl: '',
  apiKey: '',
  defaultFlag: 'NOT_DEFAULT',
  enableFlag: 'ENABLE',
  remark: '',
});

const form = reactive(initialForm());
const initialFormValue = ref({...form});
let formSession = 0;

const isDirty = computed(() => visible.value && JSON.stringify(form) !== JSON.stringify(initialFormValue.value));

const rules = computed<FormRules>(() => ({
  name: [{required: true, whitespace: true, message: t('settings.agentic.nameRequired'), trigger: 'blur'}],
  baseUrl: [{required: true, whitespace: true, message: t('settings.agentic.baseUrlRequired'), trigger: 'blur'}],
  remark: remarkRules(t),
}));

const show = () => {
  formSession += 1;
  isEdit.value = false;
  Object.assign(form, initialForm());
  initialFormValue.value = {...form};
  saveError.value = false;
  submitting.value = false;
  visible.value = true;
};

const showEdit = (row: AgenticProvider & { apiKey?: string }) => {
  formSession += 1;
  isEdit.value = true;
  Object.assign(form, initialForm(), {
    ...row,
    defaultFlag:
      String(row.defaultFlag ?? '').trim().toUpperCase() === 'DEFAULT'
        ? 'DEFAULT'
        : 'NOT_DEFAULT',
    enableFlag: enableFlagValue(row.enableFlag),
  });
  initialFormValue.value = {...form};
  saveError.value = false;
  submitting.value = false;
  visible.value = true;
};

const onClosed = () => {
  formSession += 1;
  submitting.value = false;
  formRef.value?.resetFields();
};

onBeforeUnmount(() => {
  formSession += 1;
});

const onReset = () => {
  Object.assign(form, initialFormValue.value);
  saveError.value = false;
  formRef.value?.clearValidate();
};

const requestClose = async (done?: () => void) => {
  if (submitting.value) return;
  const session = formSession;
  if (!isDirty.value) {
    if (done) done();
    else visible.value = false;
    return;
  }
  try {
    await ElMessageBox.confirm(t('common.discardConfirm'), t('common.confirm'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    });
    if (session !== formSession || !visible.value) return;
    if (done) done();
    else visible.value = false;
  } catch {
    // Keep the draft open when the user cancels the confirmation.
  }
};

const onSubmit = async () => {
  if (submitting.value) return;
  const session = formSession;
  if (!visible.value) return;
  try {
    await formRef.value?.validate();
    if (session !== formSession || !visible.value) return;
  } catch {
    if (session === formSession) submitting.value = false;
    return;
  }
  submitting.value = true;
  saveError.value = false;
  try {
    const payload = {...form};
    if (isEdit.value && !payload.apiKey?.trim()) {
      delete payload.apiKey;
    }
    emit('save', payload, (close = true) => {
      if (session !== formSession) return;
      submitting.value = false;
      if (close) {
        initialFormValue.value = {...form};
        visible.value = false;
      } else {
        saveError.value = true;
      }
    });
  } catch {
    submitting.value = false;
    saveError.value = true;
  }
};

defineExpose({show, showEdit});
</script>
