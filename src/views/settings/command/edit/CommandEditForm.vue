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
    :title="reactiveData.mode === 'add' ? $t('command.form.addTitle') : $t('command.form.editTitle')"
    class="things-dialog"
    draggable
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" class="things-form-grid" label-position="top">
      <el-form-item :label="$t('common.name')" prop="commandName">
        <el-input v-model="reactiveData.form.commandName" :placeholder="$t('common.name')" clearable />
      </el-form-item>
      <el-form-item :label="$t('command.form.code')" prop="commandCode">
        <el-input v-model="reactiveData.form.commandCode" :placeholder="$t('command.form.code')" clearable />
      </el-form-item>
      <el-form-item :label="$t('command.form.commandType')" prop="commandTypeFlag">
        <el-select v-model="reactiveData.form.commandTypeFlag" clearable>
          <el-option v-for="opt in COMMAND_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('command.form.callType')" prop="callTypeFlag">
        <el-select v-model="reactiveData.form.callTypeFlag" clearable>
          <el-option v-for="opt in CALL_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('command.form.timeout')" prop="timeout">
        <el-input-number v-model="reactiveData.form.timeout" :min="1" :precision="0" :step="1" />
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="reactiveData.form.enableFlag" />
      </el-form-item>
      <el-form-item :label="$t('common.remark')" class="things-form-grid__span-2" prop="remark">
        <el-input v-model="reactiveData.form.remark" clearable maxlength="300" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <div v-loading="reactiveData.paramLoading" class="param-editor">
      <div class="param-editor__toolbar">
        <span>{{ $t('command.form.params') }}</span>
        <el-button :icon="Plus" size="small" type="success" @click="addParamRow">
          {{ $t('common.add') }}
        </el-button>
      </div>
      <el-table :data="reactiveData.params" border max-height="260" size="small">
        <el-table-column :label="$t('common.name')" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.paramName" clearable />
          </template>
        </el-table-column>
        <el-table-column :label="$t('command.form.code')" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.paramCode" clearable />
          </template>
        </el-table-column>
        <el-table-column :label="$t('command.form.direction')" min-width="130">
          <template #default="{ row }">
            <el-select v-model="row.paramDirectionFlag">
              <el-option
                v-for="opt in PARAM_DIRECTION_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="$t('command.form.type')" min-width="130">
          <template #default="{ row }">
            <el-select v-model="row.paramTypeFlag">
              <el-option v-for="opt in POINT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="$t('command.form.required')" width="96">
          <template #default="{ row }">
            <el-checkbox v-model="row.requiredFlag" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('command.form.defaultValue')" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.defaultValue" clearable />
          </template>
        </el-table-column>
        <el-table-column :label="$t('command.form.enabled')" width="104">
          <template #default="{ row }">
            <el-switch v-model="row.enableFlag" active-value="ENABLE" inactive-value="DISABLE" />
          </template>
        </el-table-column>
        <el-table-column align="center" width="64">
          <template #default="{ $index }">
            <el-tooltip :content="$t('common.delete')" placement="top">
              <el-button :icon="Delete" link type="danger" @click="removeParamRow($index)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="things-dialog-footer">
      <el-button @click="reactiveData.visible = false">{{ $t('common.cancel') }}</el-button>
      <el-button plain @click="reset">{{ $t('common.reset') }}</el-button>
      <el-button :loading="reactiveData.submitting" type="primary" @click="submit">
        {{ $t('common.confirm') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import type { FormInstance, FormRules } from 'element-plus';
  import { Delete, Plus } from '@element-plus/icons-vue';
  import { useI18n } from 'vue-i18n';
  import { listCommandParamByCommandId } from '@/api/command';
  import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
  import {
    CALL_TYPE_OPTIONS,
    COMMAND_TYPE_OPTIONS,
    PARAM_DIRECTION_OPTIONS,
    POINT_TYPE_OPTIONS,
  } from '@/config/constant/enums';
  import type { CommandForm, CommandParamRecord, CommandRecord } from '@/config/types';
  import { failMessage } from '@/utils/notificationUtil';
  import {
    callTypeValue,
    commandTypeValue,
    enableFlagValue,
    normalizeCommandTimeoutSeconds,
    paramDirectionValue,
    pointTypeValue,
  } from '@/utils/thingModelFormatUtil';

  type FormMode = 'add' | 'edit';
  type DoneCallback = (close?: boolean) => void;

  const { t } = useI18n();

  type CommandParamDraft = CommandParamRecord & { _key: string };

  const emit = defineEmits<{
    (e: 'add-thing', form: CommandForm, params: CommandParamRecord[], done: DoneCallback): void;
    (
      e: 'update-thing',
      form: CommandForm,
      params: CommandParamRecord[],
      originalParams: CommandParamRecord[],
      done: DoneCallback
    ): void;
  }>();

  const formRef = ref<FormInstance>();

  const createEmptyForm = (profileId = '') => ({
    id: '' as string,
    profileId,
    commandName: '',
    commandCode: '',
    commandTypeFlag: 'CUSTOM' as string,
    callTypeFlag: 'SYNC' as string,
    timeout: 30,
    enableFlag: 'ENABLE' as string,
    remark: '',
  });

  const reactiveData = reactive({
    visible: false,
    mode: 'add' as FormMode,
    submitting: false,
    form: createEmptyForm(),
    originalForm: createEmptyForm(),
    params: [] as CommandParamDraft[],
    originalParams: [] as CommandParamRecord[],
    paramLoading: false,
  });

  const rules: FormRules = {
    commandName: [{ required: true, message: t('command.form.nameRequired'), trigger: 'blur' }],
    commandCode: [{ required: true, message: t('command.form.codeRequired'), trigger: 'blur' }],
  };

  const reset = () => {
    reactiveData.form = { ...reactiveData.originalForm };
    reactiveData.params = cloneParams(reactiveData.originalParams);
    reactiveData.submitting = false;
    formRef.value?.clearValidate();
  };

  const rowKey = () => `${Date.now()}-${Math.random().toString(16).slice(2)}`;

  const createEmptyParam = (): CommandParamDraft => ({
    _key: rowKey(),
    paramName: '',
    paramCode: '',
    paramDirectionFlag: 'INPUT',
    paramTypeFlag: 'STRING',
    requiredFlag: false,
    defaultValue: '',
    enableFlag: 'ENABLE',
  });

  const cloneParams = (params: CommandParamRecord[] = []): CommandParamDraft[] =>
    params.map((item) => ({
      ...item,
      _key: rowKey(),
      paramDirectionFlag: paramDirectionValue(item.paramDirectionFlag),
      paramTypeFlag: pointTypeValue(item.paramTypeFlag),
      requiredFlag: Boolean(item.requiredFlag),
      enableFlag: enableFlagValue(item.enableFlag),
    }));

  const normalizeParams = (): CommandParamRecord[] =>
    reactiveData.params
      .filter((item) => String(item.paramName || item.paramCode || '').trim() !== '')
      .map((item) => {
        const param = { ...item } as CommandParamRecord;
        delete (param as { _key?: string })._key;
        return {
          ...param,
          paramName: String(item.paramName || '').trim(),
          paramCode: String(item.paramCode || '').trim(),
          paramDirectionFlag: item.paramDirectionFlag || 'INPUT',
          paramTypeFlag: item.paramTypeFlag || 'STRING',
          requiredFlag: Boolean(item.requiredFlag),
          enableFlag: item.enableFlag || 'ENABLE',
        };
      });

  const validateParams = (params: CommandParamRecord[]) => {
    const codes = new Set<string>();
    for (const item of params) {
      const code = String(item.paramCode || '').trim();
      if (!item.paramName || !code || !item.paramDirectionFlag || !item.paramTypeFlag) {
        failMessage(t('command.form.paramRequired'));
        return false;
      }
      if (codes.has(code)) {
        failMessage(t('command.form.paramCodeUnique'));
        return false;
      }
      codes.add(code);
    }
    return true;
  };

  const addParamRow = () => {
    reactiveData.params.push(createEmptyParam());
  };

  const removeParamRow = (index: number) => {
    reactiveData.params.splice(index, 1);
  };

  const show = (profileId = '') => {
    reactiveData.mode = 'add';
    const emptyForm = createEmptyForm(profileId);
    reactiveData.originalForm = { ...emptyForm };
    reactiveData.form = { ...emptyForm };
    reactiveData.originalParams = [];
    reactiveData.params = [];
    reactiveData.visible = true;
  };

  const showEdit = (row: CommandRecord) => {
    reactiveData.mode = 'edit';
    const emptyForm = createEmptyForm();
    const initial = {
      ...emptyForm,
      ...row,
      profileId: String(row.profileId ?? emptyForm.profileId),
      commandTypeFlag: commandTypeValue(row.commandTypeFlag, emptyForm.commandTypeFlag),
      callTypeFlag: callTypeValue(row.callTypeFlag, emptyForm.callTypeFlag),
      timeout: normalizeCommandTimeoutSeconds(row.timeout) ?? emptyForm.timeout,
      enableFlag: enableFlagValue(row.enableFlag, emptyForm.enableFlag),
    };
    reactiveData.originalForm = { ...initial };
    reactiveData.form = { ...initial };
    reactiveData.originalParams = [];
    reactiveData.params = [];
    reactiveData.visible = true;
    if (row.id) {
      reactiveData.paramLoading = true;
      listCommandParamByCommandId(String(row.id))
        .then((res) => {
          reactiveData.originalParams = res.data || [];
          reactiveData.params = cloneParams(reactiveData.originalParams);
        })
        .finally(() => {
          reactiveData.paramLoading = false;
        });
    }
  };

  const done: DoneCallback = (close = true) => {
    reactiveData.submitting = false;
    if (close) {
      reactiveData.visible = false;
    }
  };

  const submit = async () => {
    const valid = await formRef.value?.validate().catch(() => false);
    if (!valid) return;
    reactiveData.submitting = true;
    const payload = {
      ...reactiveData.form,
      timeout: normalizeCommandTimeoutSeconds(reactiveData.form.timeout) ?? 30,
    };
    const params = normalizeParams();
    if (!validateParams(params)) {
      reactiveData.submitting = false;
      return;
    }
    if (reactiveData.mode === 'add') {
      emit('add-thing', payload, params, done);
    } else {
      emit('update-thing', payload, params, reactiveData.originalParams, done);
    }
  };

  defineExpose({ show, showEdit });
</script>
