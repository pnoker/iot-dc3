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
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    :title="isEdit ? $t('settings.agentic.editProvider') : $t('settings.agentic.addProvider')"
    class="things-dialog"
    draggable
    @closed="onClosed"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="$t('settings.agentic.providerName')" prop="name">
        <el-input
          v-model="form.name"
          :placeholder="$t('settings.agentic.providerNamePlaceholder')"
          clearable
          maxlength="128"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.providerType')" prop="providerType">
        <el-select
          v-model="form.providerType"
          :placeholder="$t('settings.agentic.providerTypePlaceholder')"
          style="width: 100%"
        >
          <el-option v-for="pt in providerTypes" :key="pt.value" :label="pt.label" :value="pt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.baseUrl')" prop="baseUrl">
        <el-input
          v-model="form.baseUrl"
          :placeholder="$t('settings.agentic.baseUrlPlaceholder')"
          clearable
          maxlength="256"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.apiKey')" prop="apiKey">
        <el-input
          v-model="form.apiKey"
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
          :active-text="$t('common.yes')"
          :inactive-text="$t('common.no')"
          active-value="DEFAULT"
          inactive-value="NOT_DEFAULT"
        />
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')">
        <enable-flag-segmented v-model="form.enableFlag"/>
      </el-form-item>
      <el-form-item :label="$t('common.remark')" prop="remark">
        <el-input v-model="form.remark" :rows="3" maxlength="300" show-word-limit type="textarea"/>
      </el-form-item>
    </el-form>
    <div class="things-dialog-footer">
      <el-button @click="visible = false">{{ $t('common.cancel') }}</el-button>
      <el-button plain @click="onReset">{{ $t('common.reset') }}</el-button>
      <el-button :loading="submitting" type="primary" @click="onSubmit">
        {{ $t('common.confirm') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, reactive, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import type {FormInstance, FormRules} from 'element-plus';

import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import type {AgenticProvider} from '@/config/types';
import {remarkRules} from '@/utils/formRuleUtil';

import {AGENTIC_PROVIDER_TYPES} from '../providerTypes';

const providerTypes = AGENTIC_PROVIDER_TYPES;

const emit = defineEmits<{
  (e: 'save', form: AgenticProvider & { apiKey?: string }, done: () => void): void;
}>();

const visible = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
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

const rules = computed<FormRules>(() => ({
  name: [{required: true, whitespace: true, message: t('settings.agentic.nameRequired'), trigger: 'blur'}],
  baseUrl: [{required: true, whitespace: true, message: t('settings.agentic.baseUrlRequired'), trigger: 'blur'}],
  remark: remarkRules(t),
}));

const show = () => {
  isEdit.value = false;
  Object.assign(form, initialForm());
  visible.value = true;
};

const showEdit = (row: AgenticProvider & { apiKey?: string }) => {
  isEdit.value = true;
  Object.assign(form, initialForm(), row);
  visible.value = true;
};

const onClosed = () => {
  formRef.value?.resetFields();
};

const onReset = () => {
  if (isEdit.value) {
    formRef.value?.clearValidate();
  } else {
    Object.assign(form, initialForm());
    formRef.value?.resetFields();
  }
};

const onSubmit = async () => {
  if (submitting.value) return;
  submitting.value = true;
  try {
    await formRef.value?.validate();
  } catch {
    submitting.value = false;
    return;
  }
  emit('save', {...form}, () => {
    submitting.value = false;
    visible.value = false;
  });
};

defineExpose({show, showEdit});
</script>
