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
    :title="reactiveData.mode === 'add' ? $t('eventDefinition.form.addTitle') : $t('eventDefinition.form.editTitle')"
    class="things-dialog"
    draggable
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" class="things-form-grid" label-position="top">
      <el-form-item :label="$t('common.name')" prop="eventName">
        <el-input
          v-model="reactiveData.form.eventName"
          :placeholder="$t('common.name')"
          clearable
          maxlength="32"
          show-word-limit
        />
      </el-form-item>
      <el-form-item :label="$t('eventDefinition.form.eventType')" prop="eventTypeFlag">
        <el-select v-model="reactiveData.form.eventTypeFlag" clearable>
          <el-option v-for="opt in EVENT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('eventDefinition.form.eventLevel')" prop="eventLevelFlag">
        <el-select v-model="reactiveData.form.eventLevelFlag" clearable>
          <el-option v-for="opt in EVENT_LEVEL_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
        </el-select>
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
        <span>{{ $t('eventDefinition.form.params') }}</span>
        <el-button :icon="Plus" size="small" type="success" @click="addParamRow">
          {{ $t('common.add') }}
        </el-button>
      </div>
      <el-table :data="reactiveData.params" border max-height="260" size="small">
        <el-table-column :label="$t('common.name')" min-width="170">
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
        <el-table-column :label="$t('eventDefinition.form.code')" min-width="170">
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
        <el-table-column :label="$t('eventDefinition.form.type')" min-width="140">
          <template #default="{row}">
            <el-select v-model="row.paramTypeFlag">
              <el-option v-for="opt in POINT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="$t('eventDefinition.form.enabled')" width="104">
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
import {listEventParamByEventId} from '@/api/event';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import {EVENT_LEVEL_OPTIONS, EVENT_TYPE_OPTIONS, POINT_TYPE_OPTIONS} from '@/config/constant/enums';
import type {EventForm, EventParamForm, EventParamRecord, EventRecord} from '@/config/types';
import {NAME_PATTERN, nameRules, remarkRules} from '@/utils/formRuleUtil';
import {enableFlagValue, eventLevelValue, eventTypeValue, pointTypeValue} from '@/utils/thingModelFormatUtil';

type FormMode = 'add' | 'edit';
type DoneCallback = (close?: boolean) => void;

const {t} = useI18n();

type EventParamDraft = EventParamForm & { _key: string };

const emit = defineEmits<{
  (e: 'add-thing', form: EventForm, params: EventParamRecord[], done: DoneCallback): void;
  (
    e: 'update-thing',
    form: EventForm,
    params: EventParamRecord[],
    originalParams: EventParamRecord[],
    done: DoneCallback
  ): void;
}>();

const formRef = ref<FormInstance>();

const createEmptyForm = (profileId = '') => ({
  id: '' as string,
  profileId,
  eventName: '',
  eventCode: '',
  eventTypeFlag: 'INFO' as string,
  eventLevelFlag: 'LOW' as string,
  enableFlag: 'ENABLE' as string,
  remark: '',
});

const reactiveData = reactive({
  visible: false,
  mode: 'add' as FormMode,
  submitting: false,
  form: createEmptyForm(),
  originalForm: createEmptyForm(),
  params: [] as EventParamDraft[],
  originalParams: [] as EventParamRecord[],
  paramLoading: false,
});

const rules: FormRules = {
  eventName: nameRules(t, t('common.entityEvent')),
  eventTypeFlag: [{required: true, message: t('eventDefinition.form.eventTypeRequired'), trigger: 'change'}],
  eventLevelFlag: [{required: true, message: t('eventDefinition.form.eventLevelRequired'), trigger: 'change'}],
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
    errors.paramName = t('eventDefinition.form.paramRequired');
  } else if (!NAME_PATTERN.test(name)) {
    errors.paramName = t('eventDefinition.form.paramNamePattern');
  }
  if (!code) {
    errors.paramCode = t('eventDefinition.form.paramRequired');
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

const createEmptyParam = (): EventParamDraft => ({
  _key: rowKey(),
  paramName: '',
  paramCode: '',
  paramTypeFlag: 'STRING',
  enableFlag: 'ENABLE',
});

const cloneParams = (params: EventParamRecord[] = []): EventParamDraft[] =>
  params.map((item) => ({
    ...item,
    _key: rowKey(),
    paramTypeFlag: pointTypeValue(item.paramTypeFlag),
    enableFlag: enableFlagValue(item.enableFlag),
  }));

const normalizeParams = (): EventParamRecord[] =>
  reactiveData.params.map((item) => {
    const param = {...item} as EventParamRecord;
    delete (param as { _key?: string })._key;
    return {
      ...param,
      paramName: String(item.paramName || '').trim(),
      paramCode: String(item.paramCode || '').trim(),
      paramTypeFlag: item.paramTypeFlag || 'STRING',
      enableFlag: item.enableFlag || 'ENABLE',
    };
  });

const validateParams = (params: EventParamRecord[]): boolean => {
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
    if (!item.paramName || !code || !item.paramTypeFlag) {
      valid = false;
    }
    if (item.paramName && !NAME_PATTERN.test(item.paramName)) {
      valid = false;
    }
    if (code && codes.has(code)) {
      const firstIndex = codes.get(code);
      if (firstIndex !== undefined) {
        setParamFieldError(firstIndex, 'paramCode', t('eventDefinition.form.paramCodeUnique'));
      }
      setParamFieldError(index, 'paramCode', t('eventDefinition.form.paramCodeUnique'));
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

const showEdit = (row: EventRecord) => {
  reactiveData.mode = 'edit';
  const emptyForm = createEmptyForm();
  const initial = {
    ...emptyForm,
    ...row,
    profileId: String(row.profileId ?? emptyForm.profileId),
    eventTypeFlag: eventTypeValue(row.eventTypeFlag, emptyForm.eventTypeFlag),
    eventLevelFlag: eventLevelValue(row.eventLevelFlag, emptyForm.eventLevelFlag),
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
    listEventParamByEventId(String(row.id))
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
  const payload = {...reactiveData.form};
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
