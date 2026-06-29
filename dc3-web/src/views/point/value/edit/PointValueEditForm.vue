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
    :title="$t('pointValue.edit.title')"
    class="things-dialog"
    draggable
  >
    <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule" label-position="top">
      <el-form-item :label="$t('pointValue.edit.pointValue')" prop="value">
        <el-input
          v-model="reactiveData.formData.value"
          :placeholder="$t('pointValue.edit.pointValuePlaceholder')"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('pointValue.edit.description')" prop="remark">
        <el-input
          v-model="reactiveData.formData.remark"
          :placeholder="$t('pointValue.edit.descriptionPlaceholder')"
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
        <el-button plain @click="reset">{{ $t('common.reset') }}</el-button>
        <el-button type="primary" @click="updateThing">{{ $t('common.confirm') }}</el-button>
      </slot>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
  import type {PropType} from 'vue';
  import {reactive, ref, unref} from 'vue';
  import type {FormInstance, FormRules} from 'element-plus';
  import {useI18n} from 'vue-i18n';

  import {successMessage} from '@/utils/notificationUtil';

  type PointValueFormData = Record<string, unknown> & {value?: string | number; remark?: string};

  const props = defineProps({
    formData: {
      type: Object as PropType<PointValueFormData>,
      default: () => ({}),
    },
  });

  const emit = defineEmits<{
    (e: 'update-thing', formData: PointValueFormData, done: () => void): void;
  }>();

  const {t} = useI18n();
  const formDataRef = ref<FormInstance>();

  const reactiveData = reactive({
    formVisible: false,
    formData: {} as PointValueFormData,
  });

  const formRule = reactive<FormRules>({
    value: [
      {
        required: true,
        whitespace: true,
        message: t('pointValue.edit.valueRequired'),
        trigger: 'blur',
      },
    ],
    remark: [
      {
        max: 300,
        message: t('common.remarkLength'),
        trigger: 'blur',
      },
    ],
  });

  const syncFormData = (value = props.formData) => {
    reactiveData.formData = {...value};
  };

  const show = (value?: PointValueFormData) => {
    syncFormData(value);
    reactiveData.formVisible = true;
  };

  const cancel = () => {
    reactiveData.formVisible = false;
  };

  const reset = () => {
    const form = unref(formDataRef);
    form?.resetFields();
  };

  const updateThing = async () => {
    const form = unref(formDataRef);
    if (!form) {
      return;
    }

    try {
      await form.validate();
      emit('update-thing', {...reactiveData.formData}, () => {
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
    updateThing,
  });
</script>
