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
    :title="isEdit ? $t('point.edit.title') : $t('point.add.title')"
    class="things-dialog"
    draggable
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.formData" :rules="rules" label-position="top">
      <el-form-item :label="$t('point.add.pointName')" prop="pointName">
        <el-input
          v-model="reactiveData.formData.pointName"
          :placeholder="$t('point.add.pointNamePlaceholder')"
          clearable
          maxlength="32"
          show-word-limit
        />
      </el-form-item>
      <el-form-item :label="$t('point.add.dataType')" prop="pointTypeFlag">
        <el-select
          v-model="reactiveData.formData.pointTypeFlag"
          :placeholder="$t('point.add.dataTypeRequired')"
          clearable
        >
          <el-option :label="$t('dataType.string')" value="STRING" />
          <el-option :label="$t('dataType.byte')" value="BYTE" />
          <el-option :label="$t('dataType.short')" value="SHORT" />
          <el-option :label="$t('dataType.int')" value="INT" />
          <el-option :label="$t('dataType.long')" value="LONG" />
          <el-option :label="$t('dataType.float')" value="FLOAT" />
          <el-option :label="$t('dataType.double')" value="DOUBLE" />
          <el-option :label="$t('dataType.boolean')" value="BOOLEAN" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('point.add.rwType')" prop="rwFlag">
        <el-select v-model="reactiveData.formData.rwFlag" :placeholder="$t('point.add.rwTypeRequired')" clearable>
          <el-option :label="$t('status.readOnly')" value="READ_ONLY" />
          <el-option :label="$t('status.writeOnly')" value="WRITE_ONLY" />
          <el-option :label="$t('status.readWrite')" value="READ_WRITE" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="reactiveData.formData.enableFlag" />
      </el-form-item>
      <el-form-item :label="$t('point.add.accuracy')" prop="valueDecimal">
        <el-input-number
          v-model="reactiveData.formData.valueDecimal"
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
          :placeholder="$t('point.add.unitPlaceholder')"
          clearable
          maxlength="32"
        />
      </el-form-item>
      <el-form-item :label="$t('point.add.baseValue')" prop="baseValue">
        <el-input
          v-model="reactiveData.formData.baseValue"
          :placeholder="$t('point.add.baseValuePlaceholder')"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('point.add.ratio')" prop="multiple">
        <el-input v-model="reactiveData.formData.multiple" :placeholder="$t('point.add.ratioPlaceholder')" clearable />
      </el-form-item>
      <el-form-item :label="$t('point.add.description')" prop="remark">
        <el-input
          v-model="reactiveData.formData.remark"
          :placeholder="$t('point.add.descriptionPlaceholder')"
          clearable
          maxlength="300"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <div class="things-dialog-footer">
      <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
      <el-button plain @click="formReset">{{ $t('common.reset') }}</el-button>
      <el-button :loading="reactiveData.submitting" type="primary" @click="submit">
        {{ $t('common.confirm') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
  import {reactive, ref, unref} from 'vue';
  import type {FormInstance, FormRules} from 'element-plus';
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

  const reactiveData = reactive({
    visible: false,
    submitting: false,
    formData: emptyForm('') as PointFormData,
  });

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
    isEdit.value = false;
    originalData.value = null;
    reactiveData.formData = emptyForm(profileId);
    reactiveData.visible = true;
  };

  const showEdit = (row: PointRecord) => {
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
    reactiveData.visible = true;
  };

  const cancel = () => {
    reactiveData.visible = false;
  };

  const reset = () => {
    const form = unref(formRef);
    form?.resetFields();
    reactiveData.submitting = false;
  };

  const formReset = () => {
    if (isEdit.value && originalData.value) {
      const row = originalData.value;
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
    }
    const form = unref(formRef);
    form?.clearValidate();
  };

  const submit = async () => {
    const form = unref(formRef);
    if (!form) return;

    try {
      await form.validate();
      const data = {...reactiveData.formData};
      const done: DoneCallback = (close = true) => {
        if (close) {
          reactiveData.visible = false;
        }
        reactiveData.submitting = false;
      };

      reactiveData.submitting = true;
      if (isEdit.value) {
        Object.assign(data, {id: originalData.value?.id});
        emit('update', data, done);
      } else {
        emit('add', data, done);
      }
    } catch {
      // validation errors are displayed by Element Plus
    }
  };

  defineExpose({show, showEdit, cancel, reset});
</script>
