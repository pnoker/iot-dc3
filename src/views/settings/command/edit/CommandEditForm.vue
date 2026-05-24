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
    :title="reactiveData.mode === 'add' ? $t('common.add') + ' Command' : $t('common.edit') + ' Command'"
    class="things-dialog"
    draggable
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" class="things-form-grid" label-position="top">
      <el-form-item :label="$t('common.name')" prop="commandName">
        <el-input v-model="reactiveData.form.commandName" :placeholder="$t('common.name')" clearable />
      </el-form-item>
      <el-form-item label="Code" prop="commandCode">
        <el-input v-model="reactiveData.form.commandCode" clearable placeholder="Code" />
      </el-form-item>
      <el-form-item label="Command Type" prop="commandTypeFlag">
        <el-select v-model="reactiveData.form.commandTypeFlag" clearable>
          <el-option v-for="opt in COMMAND_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="Call Type" prop="callTypeFlag">
        <el-select v-model="reactiveData.form.callTypeFlag" clearable>
          <el-option v-for="opt in CALL_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="Timeout (ms)" prop="timeout">
        <el-input-number v-model="reactiveData.form.timeout" :min="0" :step="1000" />
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="reactiveData.form.enableFlag" />
      </el-form-item>
      <el-form-item :label="$t('common.remark')" class="things-form-grid__span-2" prop="remark">
        <el-input v-model="reactiveData.form.remark" clearable maxlength="300" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <div class="things-dialog-footer">
      <el-button @click="reactiveData.visible = false">{{ $t('common.cancel') }}</el-button>
      <el-button plain type="success" @click="reset">{{ $t('common.reset') }}</el-button>
      <el-button :loading="reactiveData.submitting" type="primary" @click="submit">
        {{ $t('common.confirm') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import type { FormInstance, FormRules } from 'element-plus';
  import { useI18n } from 'vue-i18n';
  import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
  import { CALL_TYPE_OPTIONS, COMMAND_TYPE_OPTIONS } from '@/config/constant/enums';
  import type { CommandRecord } from '@/config/types';

  type FormMode = 'add' | 'edit';

  const { t } = useI18n();

  const emit = defineEmits(['add-thing', 'update-thing']);

  const formRef = ref<FormInstance>();

  const createEmptyForm = () => ({
    id: '' as string,
    commandName: '',
    commandCode: '',
    commandTypeFlag: 'CUSTOM' as string,
    callTypeFlag: 'SYNC' as string,
    timeout: 30000,
    enableFlag: 'ENABLE' as string,
    remark: '',
  });

  const reactiveData = reactive({
    visible: false,
    mode: 'add' as FormMode,
    submitting: false,
    form: createEmptyForm(),
    originalForm: createEmptyForm(),
  });

  const rules: FormRules = {
    commandName: [{ required: true, message: t('common.name'), trigger: 'blur' }],
    commandCode: [{ required: true, message: 'Code is required', trigger: 'blur' }],
  };

  const reset = () => {
    reactiveData.form = reactiveData.mode === 'edit' ? { ...reactiveData.originalForm } : createEmptyForm();
    reactiveData.submitting = false;
    formRef.value?.clearValidate();
  };

  const show = () => {
    reactiveData.mode = 'add';
    reactiveData.originalForm = createEmptyForm();
    reactiveData.form = createEmptyForm();
    reactiveData.visible = true;
  };

  const showEdit = (row: CommandRecord) => {
    reactiveData.mode = 'edit';
    const emptyForm = createEmptyForm();
    const initial = {
      ...emptyForm,
      ...row,
      commandTypeFlag: String(row.commandTypeFlag ?? emptyForm.commandTypeFlag),
      callTypeFlag: String(row.callTypeFlag ?? emptyForm.callTypeFlag),
      enableFlag: String(row.enableFlag ?? emptyForm.enableFlag),
    };
    reactiveData.originalForm = { ...initial };
    reactiveData.form = { ...initial };
    reactiveData.visible = true;
  };

  const done = () => {
    reactiveData.submitting = false;
    reactiveData.visible = false;
  };

  const submit = async () => {
    const valid = await formRef.value?.validate().catch(() => false);
    if (!valid) return;
    reactiveData.submitting = true;
    const payload = { ...reactiveData.form };
    if (reactiveData.mode === 'add') {
      emit('add-thing', payload, done);
    } else {
      emit('update-thing', payload, done);
    }
  };

  defineExpose({ show, showEdit });
</script>

<style lang="scss" scoped>
  @use '@/styles/things-dialog.scss';
</style>
