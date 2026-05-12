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
    class="things-dialog"
    draggable
    :title="isEdit ? 'Edit Model' : 'Add Model'"
    @closed="onClosed"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="Model" prop="model">
        <el-input v-model="form.model" clearable placeholder="gpt-4o-mini" />
      </el-form-item>
      <el-form-item label="Label" prop="label">
        <el-input v-model="form.label" clearable placeholder="GPT-4o Mini" />
      </el-form-item>
      <el-form-item label="Provider" prop="providerId">
        <el-select v-model="form.providerId" placeholder="Select a provider" style="width: 100%">
          <el-option v-for="p in props.providers" :key="p.id" :label="p.name" :value="p.id!" />
        </el-select>
      </el-form-item>
      <el-form-item label="Capabilities">
        <div class="agentic-form-flags">
          <el-checkbox v-model="form.stream">Stream</el-checkbox>
          <el-checkbox v-model="form.toolCall">Tools</el-checkbox>
          <el-checkbox v-model="form.vision">Vision</el-checkbox>
          <el-checkbox v-model="form.reasoning">Reasoning</el-checkbox>
        </div>
      </el-form-item>
      <el-form-item label="Temperature">
        <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" />
      </el-form-item>
      <el-form-item label="Max Tokens" prop="maxTokens">
        <el-input-number v-model="form.maxTokens" :min="1" :step="256" controls-position="right" />
      </el-form-item>
      <el-form-item label="Default">
        <el-switch
          v-model="form.defaultFlag"
          active-value="DEFAULT"
          inactive-value="NOT_DEFAULT"
          active-text="Yes"
          inactive-text="No"
        />
      </el-form-item>
      <el-form-item :label="$t('common.enable')">
        <el-switch
          v-model="form.enableFlag"
          active-value="ENABLE"
          inactive-value="DISABLE"
          :active-text="$t('common.enable')"
          :inactive-text="$t('common.disable')"
        />
      </el-form-item>
      <el-form-item :label="$t('common.remark')">
        <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="300" show-word-limit />
      </el-form-item>
    </el-form>
    <div class="things-dialog-footer">
      <el-button @click="visible = false">{{ $t('common.cancel') }}</el-button>
      <el-button plain type="success" @click="onReset">{{ $t('common.reset') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="onSubmit">
        {{ $t('common.confirm') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import type { FormInstance, FormRules } from 'element-plus';
  import type { AgenticModelConfig, AgenticProvider } from '@/config/types';

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

  const rules: FormRules = {
    model: [{ required: true, message: 'Model is required', trigger: 'blur' }],
    providerId: [{ required: true, message: 'Provider is required', trigger: 'change' }],
    maxTokens: [{ required: true, message: 'Max tokens is required', trigger: 'blur' }],
  };

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
    await formRef.value?.validate();
    submitting.value = true;
    emit('save', { ...form }, () => {
      submitting.value = false;
      visible.value = false;
    });
  };

  defineExpose({ show, showEdit });
</script>

<style lang="scss" scoped>
  @use '@/styles/things-dialog.scss';

  .agentic-form-flags {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
  }
</style>
