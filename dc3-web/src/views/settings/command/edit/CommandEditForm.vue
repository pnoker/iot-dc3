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
        <el-input
          v-model="reactiveData.form.commandName"
          :placeholder="$t('common.name')"
          clearable
          maxlength="32"
          show-word-limit
        />
      </el-form-item>
      <el-form-item :label="$t('command.form.commandType')" prop="commandTypeFlag">
        <el-select v-model="reactiveData.form.commandTypeFlag" clearable>
          <el-option v-for="opt in COMMAND_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('command.form.callType')" prop="callTypeFlag">
        <el-select v-model="reactiveData.form.callTypeFlag" clearable>
          <el-option v-for="opt in CALL_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('command.form.timeout')" prop="timeout">
        <el-input-number v-model="reactiveData.form.timeout" :min="1" :precision="0" :step="1"/>
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="reactiveData.form.enableFlag"/>
      </el-form-item>
      <el-form-item :label="$t('common.remark')" class="things-form-grid__span-2" prop="remark">
        <el-input v-model="reactiveData.form.remark" clearable maxlength="300" show-word-limit type="textarea"/>
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
          <template #default="{row, $index}">
            <div :class="{'is-error': !!paramErrors[$index]?.paramName}" class="param-field">
              <el-input
                v-model="row.paramName"
                clearable
                maxlength="32"
                show-word-limit
                @blur="validateRow($index)"
                @input="clearParamFieldError($index, 'paramName')"
              />
              <div v-if="paramErrors[$index]?.paramName" class="param-field__error">
                {{ paramErrors[$index]?.paramName }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('command.form.code')" min-width="150">
          <template #default="{row, $index}">
            <div :class="{'is-error': !!paramErrors[$index]?.paramCode}" class="param-field">
              <el-input
                v-model="row.paramCode"
                clearable
                maxlength="128"
                @blur="validateRow($index)"
                @input="clearParamFieldError($index, 'paramCode')"
              />
              <div v-if="paramErrors[$index]?.paramCode" class="param-field__error">
                {{ paramErrors[$index]?.paramCode }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('command.form.direction')" min-width="130">
          <template #default="{row}">
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
          <template #default="{row}">
            <el-select v-model="row.paramTypeFlag">
              <el-option v-for="opt in POINT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="$t('command.form.required')" width="96">
          <template #default="{row}">
            <el-checkbox v-model="row.requiredFlag"/>
          </template>
        </el-table-column>
        <el-table-column :label="$t('command.form.defaultValue')" min-width="150">
          <template #default="{row}">
            <el-input v-model="row.defaultValue" clearable maxlength="256"/>
          </template>
        </el-table-column>
        <el-table-column :label="$t('command.form.enabled')" width="104">
          <template #default="{row}">
            <el-switch v-model="row.enableFlag" active-value="ENABLE" inactive-value="DISABLE"/>
          </template>
        </el-table-column>
        <el-table-column align="center" width="64">
          <template #default="{$index}">
            <el-tooltip :content="$t('common.delete')" placement="top">
              <el-button :icon="Delete" link type="danger" @click="removeParamRow($index)"/>
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
import {reactive, ref} from 'vue';
import type {FormInstance, FormRules} from 'element-plus';
import {Delete, Plus} from '@element-plus/icons-vue';
import {useI18n} from 'vue-i18n';
import {listCommandParamByCommandId} from '@/api/command';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import {
  CALL_TYPE_OPTIONS,
  COMMAND_TYPE_OPTIONS,
  PARAM_DIRECTION_OPTIONS,
  POINT_TYPE_OPTIONS,
} from '@/config/constant/enums';
import type {CommandForm, CommandParamForm, CommandParamRecord, CommandRecord} from '@/config/types';
import {NAME_PATTERN, nameRules, remarkRules} from '@/utils/formRuleUtil';
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

const {t} = useI18n();

type CommandParamDraft = CommandParamForm & { _key: string };

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
  commandName: nameRules(t, t('common.entityCommand')),
  commandTypeFlag: [{required: true, message: t('command.form.commandTypeRequired'), trigger: 'change'}],
  callTypeFlag: [{required: true, message: t('command.form.callTypeRequired'), trigger: 'change'}],
  timeout: [{required: true, message: t('command.form.timeoutRequired'), trigger: 'blur'}],
  remark: remarkRules(t),
};

type RowErrors = { paramName?: string; paramCode?: string };
type RowErrorField = keyof RowErrors;
const paramErrors = reactive<RowErrors[]>([]);

const setParamFieldError = (index: number, field: RowErrorField, message: string) => {
  paramErrors[index] = {...(paramErrors[index] || {}), [field]: message};
};

const clearParamFieldError = (index: number, field: RowErrorField) => {
  if (!paramErrors[index]?.[field]) return;
  paramErrors[index] = {...(paramErrors[index] || {}), [field]: undefined};
};

const validateRow = (index: number): boolean => {
  const row = reactiveData.params[index];
  if (!row) return true;
  const errors: RowErrors = {};
  const name = String(row.paramName || '').trim();
  const code = String(row.paramCode || '').trim();
  if (!name) {
    errors.paramName = t('command.form.paramRequired');
  } else if (!NAME_PATTERN.test(name)) {
    errors.paramName = t('command.form.paramNamePattern');
  }
  if (!code) {
    errors.paramCode = t('command.form.paramRequired');
  }
  paramErrors[index] = errors;
  return !errors.paramName && !errors.paramCode;
};

const clearParamErrors = () => {
  paramErrors.splice(0, paramErrors.length);
};

const reset = () => {
  reactiveData.form = {...reactiveData.originalForm};
  reactiveData.params = cloneParams(reactiveData.originalParams);
  reactiveData.submitting = false;
  formRef.value?.clearValidate();
  clearParamErrors();
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
  reactiveData.params.map((item) => {
    const param = {...item} as CommandParamRecord;
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

const validateParams = (params: CommandParamRecord[]): boolean => {
  clearParamErrors();
  let valid = true;
  const codes = new Map<string, number>();
  for (let i = 0; i < reactiveData.params.length; i++) {
    if (!validateRow(i)) {
      valid = false;
    }
  }
  for (let index = 0; index < params.length; index++) {
    const item = params[index];
    if (!item) continue;
    const code = String(item.paramCode || '').trim();
    if (!item.paramName || !code || !item.paramDirectionFlag || !item.paramTypeFlag) {
      valid = false;
    }
    if (item.paramName && !NAME_PATTERN.test(item.paramName)) {
      valid = false;
    }
    if (code && codes.has(code)) {
      const firstIndex = codes.get(code);
      if (firstIndex !== undefined) {
        setParamFieldError(firstIndex, 'paramCode', t('command.form.paramCodeUnique'));
      }
      setParamFieldError(index, 'paramCode', t('command.form.paramCodeUnique'));
      valid = false;
    }
    if (code && !codes.has(code)) {
      codes.set(code, index);
    }
  }
  return valid;
};

const addParamRow = () => {
  reactiveData.params.push(createEmptyParam());
};

const removeParamRow = (index: number) => {
  reactiveData.params.splice(index, 1);
  paramErrors.splice(index, 1);
};

const show = (profileId = '') => {
  reactiveData.mode = 'add';
  const emptyForm = createEmptyForm(profileId);
  reactiveData.originalForm = {...emptyForm};
  reactiveData.form = {...emptyForm};
  reactiveData.originalParams = [];
  reactiveData.params = [];
  clearParamErrors();
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
  reactiveData.originalForm = {...initial};
  reactiveData.form = {...initial};
  reactiveData.originalParams = [];
  reactiveData.params = [];
  clearParamErrors();
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
  if (reactiveData.submitting) return;
  reactiveData.submitting = true;
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    reactiveData.submitting = false;
    return;
  }
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

defineExpose({show, showEdit});
</script>

<style>
.is-error .el-input__wrapper {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset !important;
}

.param-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 2px 0;
}

.param-field__error {
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.2;
}
</style>
