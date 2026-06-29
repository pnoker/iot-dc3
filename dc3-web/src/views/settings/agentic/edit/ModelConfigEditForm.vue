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
    v-model="visible"
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    :title="isEdit ? $t('settings.agentic.editModel') : $t('settings.agentic.addModel')"
    class="things-dialog"
    draggable
    @closed="onClosed"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="$t('settings.agentic.model')" prop="model">
        <el-input
          v-model="form.model"
          :placeholder="$t('settings.agentic.modelPlaceholder')"
          clearable
          maxlength="128"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.label')" prop="label">
        <el-input
          v-model="form.label"
          :placeholder="$t('settings.agentic.labelPlaceholder')"
          clearable
          maxlength="128"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.provider')" prop="providerId">
        <el-select
          v-model="form.providerId"
          :placeholder="$t('settings.agentic.providerPlaceholder')"
          style="width: 100%"
        >
          <el-option v-for="p in props.providers" :key="p.id" :label="p.name" :value="p.id!" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.capabilities')">
        <div class="agentic-form-flags">
          <el-checkbox v-model="form.stream">{{ $t('agentic.capStream') }}</el-checkbox>
          <el-checkbox v-model="form.toolCall">{{ $t('agentic.capTools') }}</el-checkbox>
          <el-checkbox v-model="form.vision">{{ $t('agentic.capVision') }}</el-checkbox>
          <el-checkbox v-model="form.reasoning">{{ $t('agentic.capReasoning') }}</el-checkbox>
        </div>
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.temperature')" prop="temperature">
        <el-slider v-model="form.temperature" :max="2" :min="0" :step="0.1" />
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.maxTokens')" prop="maxTokens">
        <el-input-number v-model="form.maxTokens" :min="1" :precision="0" :step="256" controls-position="right" />
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
        <enable-flag-segmented v-model="form.enableFlag" />
      </el-form-item>
      <el-form-item :label="$t('common.remark')" prop="remark">
        <el-input v-model="form.remark" :rows="3" maxlength="300" show-word-limit type="textarea" />
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
  import type {AgenticModelConfig, AgenticProvider} from '@/config/types';
  import {remarkRules} from '@/utils/formRuleUtil';

  const props = defineProps<{
    providers: AgenticProvider[];
  }>();

  const emit = defineEmits<{
    (e: 'save', form: AgenticModelConfig, done: () => void): void;
  }>();

  const visible = ref(false);
  const isEdit = ref(false);
  const submitting = ref(false);
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
    isEdit.value = false;
    Object.assign(form, initialForm());
    visible.value = true;
  };

  const showEdit = (row: AgenticModelConfig) => {
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

<style lang="scss" scoped>
  .agentic-form-flags {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
  }
</style>
