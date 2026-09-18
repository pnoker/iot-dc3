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
    :title="isEdit ? $t('settings.agentic.editModel') : $t('settings.agentic.addModel')"
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
      <el-form-item :label="$t('settings.agentic.model')" prop="model">
        <el-input
          v-model="form.model"
          :disabled="submitting"
          :placeholder="$t('settings.agentic.modelPlaceholder')"
          clearable
          maxlength="128"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.label')" prop="label">
        <el-input
          v-model="form.label"
          :disabled="submitting"
          :placeholder="$t('settings.agentic.labelPlaceholder')"
          clearable
          maxlength="128"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.provider')" prop="providerId">
        <el-select
          v-model="form.providerId"
          :disabled="submitting"
          :placeholder="$t('settings.agentic.providerPlaceholder')"
          style="width: 100%"
        >
          <el-option v-for="p in props.providers" :key="p.id" :label="p.name" :value="p.id!"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.capabilities')">
        <div class="agentic-form-flags">
          <el-checkbox v-model="form.stream" :disabled="submitting">{{ $t('agentic.capStream') }}</el-checkbox>
          <el-checkbox v-model="form.toolCall" :disabled="submitting">{{ $t('agentic.capTools') }}</el-checkbox>
          <el-checkbox v-model="form.vision" :disabled="submitting">{{ $t('agentic.capVision') }}</el-checkbox>
          <el-checkbox v-model="form.reasoning" :disabled="submitting">{{ $t('agentic.capReasoning') }}</el-checkbox>
        </div>
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.temperature')" prop="temperature">
        <el-slider v-model="form.temperature" :disabled="submitting" :max="2" :min="0" :step="0.1"/>
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.maxTokens')" prop="maxTokens">
        <el-input-number v-model="form.maxTokens" :disabled="submitting" :min="1" :precision="0" :step="256" controls-position="right"/>
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
import type {AgenticModelConfig, AgenticProvider} from '@/config/types';
import {remarkRules} from '@/utils/formRuleUtil';
import {enableFlagValue} from '@/utils/thingModelFormatUtil';

const props = defineProps<{
  providers: AgenticProvider[];
}>();

const emit = defineEmits<{
  (e: 'save', form: AgenticModelConfig, done: (close?: boolean) => void): void;
}>();

const visible = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
const saveError = ref(false);
const formRef = ref<FormInstance>();
const {t} = useI18n();

const initialForm = (): AgenticModelConfig => ({
  model: '',
  label: '',
  providerId: '',
  stream: true,
  toolCall: true,
  vision: false,
  reasoning: false,
  temperature: 0.7,
  maxTokens: 2048,
  defaultFlag: 'NOT_DEFAULT',
  enableFlag: 'ENABLE',
  remark: '',
});

const form = reactive<AgenticModelConfig>(initialForm());
const initialFormValue = ref({...form});
let formSession = 0;

const isDirty = computed(() => visible.value && JSON.stringify(form) !== JSON.stringify(initialFormValue.value));

const rules = computed<FormRules>(() => ({
  model: [{required: true, whitespace: true, message: t('settings.agentic.modelRequired'), trigger: 'blur'}],
  providerId: [{required: true, message: t('settings.agentic.providerRequired'), trigger: 'change'}],
  maxTokens: [{required: true, message: t('settings.agentic.maxTokensRequired'), trigger: 'blur'}],
  temperature: [
    {
      type: 'number',
      min: 0,
      max: 2,
      message: t('settings.agentic.temperatureRange'),
      trigger: 'change',
    },
  ],
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

const showEdit = (row: AgenticModelConfig) => {
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
    emit('save', {...form}, (close = true) => {
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

<style lang="scss" scoped>
.agentic-form-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
</style>
