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
    :before-close="requestClose"
    :close-on-click-modal="false"
    :close-on-press-escape="!reactiveData.submitting"
    :show-close="!reactiveData.submitting"
    :title="isEdit ? $t('point.edit.title') : $t('point.add.title')"
    class="things-dialog"
    destroy-on-close
    draggable
    @closed="onClosed"
  >
    <el-alert
      v-if="reactiveData.saveError"
      :closable="false"
      :title="$t('common.saveFailed')"
      class="things-dialog-form-alert"
      show-icon
      type="error"
    />
    <el-form
      ref="formRef"
      v-loading="reactiveData.submitting"
      :aria-busy="reactiveData.submitting"
      :model="reactiveData.formData"
      :rules="rules"
      label-position="top"
    >
      <el-form-item :label="$t('point.add.pointName')" prop="pointName">
        <el-input
          v-model="reactiveData.formData.pointName"
          :disabled="reactiveData.submitting"
          :placeholder="$t('point.add.pointNamePlaceholder')"
          clearable
          maxlength="32"
          show-word-limit
        />
      </el-form-item>
      <el-form-item :label="$t('point.add.dataType')" prop="pointTypeFlag">
        <el-select
          v-model="reactiveData.formData.pointTypeFlag"
          :disabled="reactiveData.submitting"
          :placeholder="$t('point.add.dataTypeRequired')"
          clearable
        >
          <el-option :label="$t('dataType.string')" value="STRING"/>
          <el-option :label="$t('dataType.byte')" value="BYTE"/>
          <el-option :label="$t('dataType.short')" value="SHORT"/>
          <el-option :label="$t('dataType.int')" value="INT"/>
          <el-option :label="$t('dataType.long')" value="LONG"/>
          <el-option :label="$t('dataType.float')" value="FLOAT"/>
          <el-option :label="$t('dataType.double')" value="DOUBLE"/>
          <el-option :label="$t('dataType.boolean')" value="BOOLEAN"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('point.add.rwType')" prop="rwFlag">
        <el-select
          v-model="reactiveData.formData.rwFlag"
          :disabled="reactiveData.submitting"
          :placeholder="$t('point.add.rwTypeRequired')"
          clearable
        >
          <el-option :label="$t('status.readOnly')" value="READ_ONLY"/>
          <el-option :label="$t('status.writeOnly')" value="WRITE_ONLY"/>
          <el-option :label="$t('status.readWrite')" value="READ_WRITE"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="reactiveData.formData.enableFlag" :disabled="reactiveData.submitting"/>
      </el-form-item>
      <el-form-item :label="$t('point.add.accuracy')" prop="valueDecimal">
        <el-input-number
          v-model="reactiveData.formData.valueDecimal"
          :disabled="reactiveData.submitting"
          :max="127"
          :min="0"
          :placeholder="$t('point.add.accuracyPlaceholder')"
          :precision="0"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item :label="$t('point.add.unit')" prop="unit">
        <el-input
          v-model="reactiveData.formData.unit"
          :disabled="reactiveData.submitting"
          :placeholder="$t('point.add.unitPlaceholder')"
          clearable
          maxlength="32"
        />
      </el-form-item>
      <el-form-item :label="$t('point.add.baseValue')" prop="baseValue">
        <el-input
          v-model="reactiveData.formData.baseValue"
          :disabled="reactiveData.submitting"
          :placeholder="$t('point.add.baseValuePlaceholder')"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('point.add.ratio')" prop="multiple">
        <el-input
          v-model="reactiveData.formData.multiple"
          :disabled="reactiveData.submitting"
          :placeholder="$t('point.add.ratioPlaceholder')"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('point.add.description')" prop="remark">
        <el-input
          v-model="reactiveData.formData.remark"
          :disabled="reactiveData.submitting"
          :placeholder="$t('point.add.descriptionPlaceholder')"
          clearable
          maxlength="300"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="things-dialog-footer">
        <el-button :disabled="reactiveData.submitting" @click="cancel">{{ $t('common.cancel') }}</el-button>
        <el-button :disabled="reactiveData.submitting" plain @click="formReset">{{ $t('common.reset') }}</el-button>
        <el-button :loading="reactiveData.submitting" type="primary" @click="submit">
          {{ $t('common.confirm') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, reactive, ref, unref} from 'vue';
import type {FormInstance, FormRules} from 'element-plus';
import {ElMessageBox} from 'element-plus';
import {useI18n} from 'vue-i18n';

import {byteRules, decimalRules, nameRules, remarkRules, requiredSelectRule} from '@/utils/formRuleUtil';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';

import type {PointRecord} from '@/config/types';

interface PointFormData {
  pointName?: string;
  pointTypeFlag: string;
  rwFlag: string;
  baseValue: number;
  multiple: number;
  valueDecimal: number;
  unit: string;
  enableFlag: string;
  profileId: string;
  remark?: string;
}

type DoneCallback = (close?: boolean) => void;

const emit = defineEmits<{
  (e: 'add', form: PointFormData, done: DoneCallback): void;
  (e: 'update', form: PointFormData, done: DoneCallback): void;
}>();

const {t} = useI18n();
const formRef = ref<FormInstance>();

const isEdit = ref(false);
const originalData = ref<PointRecord | null>(null);
let formSession = 0;

const emptyForm = (profileId: string): PointFormData => ({
  pointTypeFlag: 'FLOAT',
  rwFlag: 'READ_ONLY',
  baseValue: 0,
  multiple: 1,
  valueDecimal: 3,
  unit: '',
  enableFlag: 'ENABLE',
  profileId,
});

const initialData = ref<PointFormData>(emptyForm(''));

const reactiveData = reactive({
  visible: false,
  submitting: false,
  saveError: false,
  formData: emptyForm('') as PointFormData,
});

const isDirty = computed(
  () => reactiveData.visible && JSON.stringify(reactiveData.formData) !== JSON.stringify(initialData.value)
);

const rules = reactive<FormRules>({
  pointName: nameRules(t, t('common.entityPoint')),
  pointTypeFlag: requiredSelectRule(t('point.add.dataTypeRequired')),
  rwFlag: requiredSelectRule(t('point.add.rwTypeRequired')),
  enableFlag: [{message: t('common.enableFlag'), trigger: 'change'}],
  baseValue: decimalRules(t('point.add.baseValueFormat')),
  multiple: decimalRules(t('point.add.ratioFormat')),
  valueDecimal: byteRules(t, t('point.add.accuracyFormat')),
  remark: remarkRules(t),
});

const show = (profileId: string) => {
  formSession += 1;
  isEdit.value = false;
  originalData.value = null;
  reactiveData.formData = emptyForm(profileId);
  initialData.value = {...reactiveData.formData};
  reactiveData.submitting = false;
  reactiveData.saveError = false;
  reactiveData.visible = true;
};

const showEdit = (row: PointRecord) => {
  formSession += 1;
  isEdit.value = true;
  originalData.value = {...row};
  reactiveData.formData = {
    pointName: row.pointName,
    pointTypeFlag: row.pointTypeFlag,
    rwFlag: row.rwFlag,
    baseValue: row.baseValue,
    multiple: row.multiple,
    valueDecimal: row.valueDecimal,
    unit: row.unit || '',
    enableFlag: row.enableFlag,
    profileId: row.profileId,
    remark: row.remark,
  } as PointFormData;
  initialData.value = {...reactiveData.formData};
  reactiveData.submitting = false;
  reactiveData.saveError = false;
  reactiveData.visible = true;
};

const cancel = () => {
  void requestClose();
};

const reset = () => {
  formReset();
};

const onClosed = () => {
  formSession += 1;
  reactiveData.submitting = false;
  formRef.value?.clearValidate();
};

onBeforeUnmount(() => {
  formSession += 1;
});

const requestClose = async (done?: () => void) => {
  if (reactiveData.submitting) return;
  const session = formSession;
  if (!isDirty.value) {
    if (done) done();
    else reactiveData.visible = false;
    return;
  }
  try {
    await ElMessageBox.confirm(t('common.discardConfirm'), t('common.confirm'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    });
    if (session !== formSession || !reactiveData.visible) return;
    if (done) done();
    else reactiveData.visible = false;
  } catch {
    // Keep the draft open when the user cancels the confirmation.
  }
};

const formReset = () => {
  reactiveData.formData = {...initialData.value};
  reactiveData.saveError = false;
  const form = unref(formRef);
  form?.clearValidate();
};

const submit = async () => {
  if (reactiveData.submitting) return;
  const form = unref(formRef);
  if (!form) return;

  const session = formSession;
  try {
    await form.validate();
    if (session !== formSession || !reactiveData.visible) return;
    const data = {...reactiveData.formData};
    const done: DoneCallback = (close = true) => {
      if (session !== formSession) return;
      reactiveData.submitting = false;
      if (close) {
        reactiveData.visible = false;
        initialData.value = {...reactiveData.formData};
      } else {
        reactiveData.saveError = true;
      }
    };

    reactiveData.submitting = true;
    reactiveData.saveError = false;
    try {
      if (isEdit.value) {
        Object.assign(data, {id: originalData.value?.id});
        emit('update', data, done);
      } else {
        emit('add', data, done);
      }
    } catch {
      reactiveData.submitting = false;
      reactiveData.saveError = true;
    }
  } catch {
    // validation errors are displayed by Element Plus
  }
};

defineExpose({show, showEdit, cancel, reset});
</script>
