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
    v-model="reactiveData.formVisible"
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    :title="$t('point.add.title')"
    class="things-dialog"
    draggable
  >
    <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule" label-position="top">
      <el-form-item :label="$t('point.add.pointName')" prop="pointName">
        <el-input
          v-model="reactiveData.formData.pointName"
          :placeholder="$t('point.add.pointNamePlaceholder')"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('point.add.dataType')" prop="pointTypeFlag">
        <el-select
          v-model="reactiveData.formData.pointTypeFlag"
          :placeholder="$t('point.add.dataTypeRequired')"
          clearable
        >
          <el-option :label="$t('dataType.string')" value="STRING"></el-option>
          <el-option :label="$t('dataType.byte')" value="BYTE"></el-option>
          <el-option :label="$t('dataType.short')" value="SHORT"></el-option>
          <el-option :label="$t('dataType.int')" value="INT"></el-option>
          <el-option :label="$t('dataType.long')" value="LONG"></el-option>
          <el-option :label="$t('dataType.float')" value="FLOAT"></el-option>
          <el-option :label="$t('dataType.double')" value="DOUBLE"></el-option>
          <el-option :label="$t('dataType.boolean')" value="BOOLEAN"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('point.add.rwType')" prop="rwFlag">
        <el-select v-model="reactiveData.formData.rwFlag" :placeholder="$t('point.add.rwTypeRequired')" clearable>
          <el-option :label="$t('status.readOnly')" value="R"></el-option>
          <el-option :label="$t('status.writeOnly')" value="W"></el-option>
          <el-option :label="$t('status.readWrite')" value="RW"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('point.add.accuracy')" prop="valueDecimal">
        <el-input
          v-model="reactiveData.formData.valueDecimal"
          :placeholder="$t('point.add.accuracyPlaceholder')"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('point.add.unit')" prop="unit">
        <el-input
          v-model="reactiveData.formData.unit"
          :placeholder="$t('point.add.unitPlaceholder')"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('point.add.baseValue')" prop="baseValue">
        <el-input
          v-model="reactiveData.formData.baseValue"
          :placeholder="$t('point.add.baseValuePlaceholder')"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('point.add.ratio')" prop="multiple">
        <el-input
          v-model="reactiveData.formData.multiple"
          :placeholder="$t('point.add.ratioPlaceholder')"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('point.add.description')" prop="remark">
        <el-input
          v-model="reactiveData.formData.remark"
          :placeholder="$t('point.add.descriptionPlaceholder')"
          clearable
          maxlength="300"
          show-word-limit
          type="textarea"
        ></el-input>
      </el-form-item>
    </el-form>
    <div class="things-dialog-footer">
      <slot name="footer">
        <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
        <el-button plain type="success" @click="reset">{{ $t('common.reset') }}</el-button>
        <el-button type="primary" @click="addThing">{{ $t('common.confirm') }}</el-button>
      </slot>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
  import { reactive, ref, unref } from 'vue';
  import type { FormInstance, FormRules } from 'element-plus';
  import { useI18n } from 'vue-i18n';

  import { successMessage } from '@/utils/notificationUtil';
  import { DECIMAL_PATTERN, nameRules, remarkRules } from '@/utils/formRuleUtil';

  interface PointAddFormData {
    pointName?: string;
    pointTypeFlag: string;
    rwFlag: string;
    baseValue: number;
    multiple: number;
    valueDecimal: number;
    unit: string;
    profileId: string;
    remark?: string;
  }

  const props = withDefaults(
    defineProps<{
      profileId?: string;
    }>(),
    {
      profileId: '',
    }
  );

  const emit = defineEmits<{
    (e: 'add-thing', formData: PointAddFormData, done: () => void): void;
  }>();

  const { t } = useI18n();
  const formDataRef = ref<FormInstance>();

  const reactiveData = reactive({
    formData: {
      pointTypeFlag: 'FLOAT',
      rwFlag: 'R',
      baseValue: 0,
      multiple: 1,
      valueDecimal: 3,
      unit: '',
      profileId: props.profileId,
    } as PointAddFormData,
    formVisible: false,
  });

  const formRule = reactive<FormRules>({
    pointName: nameRules(t, '位号'),
    pointTypeFlag: [
      {
        required: true,
        message: t('point.add.dataTypeRequired'),
        trigger: 'change',
      },
    ],
    rwFlag: [
      {
        required: true,
        message: t('point.add.rwTypeRequired'),
        trigger: 'change',
      },
    ],
    baseValue: [
      {
        pattern: DECIMAL_PATTERN,
        message: t('point.add.baseValueFormat'),
      },
    ],
    multiple: [
      {
        pattern: DECIMAL_PATTERN,
        message: t('point.add.ratioFormat'),
      },
    ],
    valueDecimal: [
      {
        required: true,
        message: t('point.add.accuracyFormat'),
        trigger: 'blur',
      },
    ],
    remark: remarkRules(t),
  });

  const show = () => {
    reactiveData.formVisible = true;
  };

  const cancel = () => {
    reactiveData.formVisible = false;
  };

  const reset = () => {
    const form = unref(formDataRef);
    form?.resetFields();
  };

  const addThing = async () => {
    const form = unref(formDataRef);
    if (!form) {
      return;
    }

    try {
      await form.validate();
      emit('add-thing', { ...reactiveData.formData }, () => {
        cancel();
        reset();
        successMessage();
      });
    } catch {
      // validation errors are displayed by Element Plus
    }
  };

  defineExpose({
    show,
    cancel,
    reset,
    addThing,
  });
</script>

<style lang="scss" scoped>
  @use '@/styles/things-dialog.scss';
</style>
