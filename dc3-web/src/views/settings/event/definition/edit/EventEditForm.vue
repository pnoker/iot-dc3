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
    :aria-busy="reactiveData.submitting || reactiveData.paramLoading"
    :append-to-body="true"
    :before-close="requestClose"
    :close-on-click-modal="!reactiveData.submitting"
    :close-on-press-escape="!reactiveData.submitting"
    :show-close="!reactiveData.submitting"
    :title="reactiveData.mode === 'add' ? $t('eventDefinition.form.addTitle') : $t('eventDefinition.form.editTitle')"
    class="things-dialog things-dialog--wide"
    destroy-on-close
    draggable
    @closed="reset"
  >
    <el-alert
      v-if="reactiveData.paramError"
      :closable="false"
      :title="$t('common.loadFailed')"
      class="things-dialog-form-alert"
      show-icon
      type="error"
    >
      <el-button :loading="reactiveData.paramLoading" link type="danger" @click="retryParams">
        {{ $t('common.retry') }}
      </el-button>
    </el-alert>
    <el-alert
      v-if="reactiveData.saveError"
      :closable="false"
      :title="$t('common.saveFailed')"
      class="things-dialog-form-alert"
      show-icon
      type="error"
    />
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" class="things-form-grid" label-position="top">
      <el-form-item :label="$t('common.name')" prop="eventName">
        <el-input
          v-model="reactiveData.form.eventName"
          :disabled="reactiveData.submitting"
          :placeholder="$t('common.name')"
          clearable
          maxlength="32"
          show-word-limit
        />
      </el-form-item>
      <el-form-item :label="$t('eventDefinition.form.eventType')" prop="eventTypeFlag">
        <el-select v-model="reactiveData.form.eventTypeFlag" :disabled="reactiveData.submitting" clearable>
          <el-option v-for="opt in EVENT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('eventDefinition.form.eventLevel')" prop="eventLevelFlag">
        <el-select v-model="reactiveData.form.eventLevelFlag" :disabled="reactiveData.submitting" clearable>
          <el-option v-for="opt in EVENT_LEVEL_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="reactiveData.form.enableFlag" :disabled="reactiveData.submitting"/>
      </el-form-item>
      <el-form-item :label="$t('common.remark')" class="things-form-grid__span-2" prop="remark">
        <el-input
          v-model="reactiveData.form.remark"
          :disabled="reactiveData.submitting"
          clearable
          maxlength="300"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <div v-loading="reactiveData.paramLoading" class="param-editor">
      <div class="param-editor__toolbar">
        <span>{{ $t('eventDefinition.form.params') }}</span>
        <el-button
          :disabled="reactiveData.paramLoading || reactiveData.submitting"
          :icon="Plus"
          type="success"
          @click="addParamRow"
        >
          {{ $t('common.add') }}
        </el-button>
      </div>
      <el-empty
        v-if="!reactiveData.paramLoading && !reactiveData.paramError && reactiveData.params.length === 0"
        :description="$t('common.empty')"
      />
      <div v-else class="param-editor__rows">
        <article v-for="(row, index) in reactiveData.params" :key="row._key" class="param-editor__row">
          <header class="param-editor__row-header">
            <strong class="param-editor__row-title">{{ $t('eventDefinition.form.params') }} #{{ index + 1 }}</strong>
            <el-button
              :disabled="reactiveData.submitting"
              :icon="Delete"
              plain
              type="danger"
              @click="removeParamRow(index)"
            >
              {{ $t('common.delete') }}
            </el-button>
          </header>
          <div class="param-editor__fields">
            <div :class="{'is-error': !!paramErrors[index]?.paramName}" class="param-editor__field param-field">
              <label :for="`event-param-name-${row._key}`" class="param-editor__label">{{ $t('common.name') }}</label>
              <el-input
                :id="`event-param-name-${row._key}`"
                v-model="row.paramName"
                :disabled="reactiveData.submitting"
                clearable
                maxlength="32"
                show-word-limit
                @blur="validateRow(index)"
                @input="clearParamFieldError(index, 'paramName')"
              />
              <div v-if="paramErrors[index]?.paramName" class="param-field__error">
                {{ paramErrors[index]?.paramName }}
              </div>
            </div>
            <div :class="{'is-error': !!paramErrors[index]?.paramCode}" class="param-editor__field param-field">
              <label :for="`event-param-code-${row._key}`" class="param-editor__label">
                {{ $t('eventDefinition.form.code') }}
              </label>
              <el-input
                :id="`event-param-code-${row._key}`"
                v-model="row.paramCode"
                :disabled="reactiveData.submitting"
                clearable
                maxlength="128"
                @blur="validateRow(index)"
                @input="clearParamFieldError(index, 'paramCode')"
              />
              <div v-if="paramErrors[index]?.paramCode" class="param-field__error">
                {{ paramErrors[index]?.paramCode }}
              </div>
            </div>
            <div class="param-editor__field">
              <label class="param-editor__label">{{ $t('eventDefinition.form.type') }}</label>
              <el-select v-model="row.paramTypeFlag" :disabled="reactiveData.submitting">
                <el-option v-for="opt in POINT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value"/>
              </el-select>
            </div>
            <div class="param-editor__field param-editor__field--toggle">
              <span class="param-editor__label">{{ $t('eventDefinition.form.enabled') }}</span>
              <el-switch v-model="row.enableFlag" :disabled="reactiveData.submitting" active-value="ENABLE" inactive-value="DISABLE"/>
            </div>
          </div>
        </article>
      </div>
    </div>
    <template #footer>
      <div class="things-dialog-footer">
        <el-button :disabled="reactiveData.submitting" @click="requestClose()">{{ $t('common.cancel') }}</el-button>
        <el-button :disabled="reactiveData.submitting || reactiveData.paramLoading" plain @click="reset">
          {{ $t('common.reset') }}
        </el-button>
        <el-button
          :disabled="Boolean(reactiveData.paramError) || reactiveData.paramLoading"
          :loading="reactiveData.submitting"
          type="primary"
          @click="submit"
        >
          {{ $t('common.confirm') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {ElMessageBox} from 'element-plus';
import {computed, onBeforeUnmount, reactive, ref} from 'vue';
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
  paramError: null as unknown | null,
  saveError: null as unknown | null,
});

const initialSnapshot = ref('');
let formSessionId = 0;
let paramRequestId = 0;

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

const snapshot = () => JSON.stringify({form: reactiveData.form, params: normalizeParams()});
const captureSnapshot = () => {
  initialSnapshot.value = snapshot();
  reactiveData.saveError = null;
};
const formDirty = computed(() => reactiveData.visible && snapshot() !== initialSnapshot.value);

const reset = () => {
  reactiveData.form = {...reactiveData.originalForm};
  reactiveData.params = cloneParams(reactiveData.originalParams);
  reactiveData.submitting = false;
  reactiveData.saveError = null;
  formRef.value?.clearValidate();
  clearParamErrors();
  captureSnapshot();
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

const loadParams = async (entityId: string, sessionId: number) => {
  if (!entityId) {
    reactiveData.paramLoading = false;
    reactiveData.paramError = null;
    captureSnapshot();
    return;
  }
  const requestId = ++paramRequestId;
  reactiveData.paramLoading = true;
  reactiveData.paramError = null;
  try {
    const res = await listEventParamByEventId(entityId);
    if (requestId !== paramRequestId || sessionId !== formSessionId || !reactiveData.visible) return;
    reactiveData.originalParams = res || [];
    reactiveData.params = cloneParams(reactiveData.originalParams);
    captureSnapshot();
  } catch (error) {
    if (requestId === paramRequestId && sessionId === formSessionId && reactiveData.visible) {
      reactiveData.paramError = error;
    }
  } finally {
    if (requestId === paramRequestId && sessionId === formSessionId) reactiveData.paramLoading = false;
  }
};

const retryParams = async () => {
  if (reactiveData.mode !== 'edit' || !reactiveData.form.id) return;
  await loadParams(String(reactiveData.form.id), formSessionId);
};

const show = (profileId = '') => {
  formSessionId += 1;
  paramRequestId += 1;
  reactiveData.mode = 'add';
  const emptyForm = createEmptyForm(profileId);
  reactiveData.originalForm = {...emptyForm};
  reactiveData.form = {...emptyForm};
  reactiveData.originalParams = [];
  reactiveData.params = [];
  reactiveData.paramLoading = false;
  reactiveData.paramError = null;
  reactiveData.saveError = null;
  clearParamErrors();
  reactiveData.visible = true;
  captureSnapshot();
};

const showEdit = (row: EventRecord) => {
  formSessionId += 1;
  paramRequestId += 1;
  const sessionId = formSessionId;
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
  reactiveData.paramError = null;
  reactiveData.saveError = null;
  clearParamErrors();
  reactiveData.visible = true;
  reactiveData.paramLoading = Boolean(row.id);
  initialSnapshot.value = snapshot();
  void loadParams(String(row.id || ''), sessionId);
};

const finishClose = (done?: () => void) => {
  formSessionId += 1;
  paramRequestId += 1;
  reactiveData.submitting = false;
  reactiveData.saveError = null;
  if (done) {
    done();
    return;
  }
  reactiveData.visible = false;
};

const requestClose = async (done?: () => void) => {
  if (reactiveData.submitting) return;
  const session = formSessionId;
  if (!formDirty.value) {
    finishClose(done);
    return;
  }
  try {
    await ElMessageBox.confirm(t('common.discardConfirm'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    });
    if (session !== formSessionId || !reactiveData.visible) return;
    finishClose(done);
  } catch {
    // Keep the draft open when the user cancels the confirmation.
  }
};

const finish = (sessionId: number, close = true) => {
  if (sessionId !== formSessionId || !reactiveData.visible) return;
  reactiveData.submitting = false;
  if (close) {
    captureSnapshot();
    reactiveData.visible = false;
  } else {
    reactiveData.saveError = new Error(t('common.saveFailed'));
  }
};

const submit = async () => {
  if (reactiveData.submitting) return;
  const sessionId = formSessionId;
  reactiveData.submitting = true;
  const valid = await formRef.value?.validate().catch(() => false);
  if (sessionId !== formSessionId || !reactiveData.visible) return;
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
  const done: DoneCallback = (close = true) => finish(sessionId, close);
  if (reactiveData.mode === 'add') {
    emit('add-thing', payload, params, done);
  } else {
    emit('update-thing', payload, params, reactiveData.originalParams, done);
  }
};

onBeforeUnmount(() => {
  formSessionId += 1;
  paramRequestId += 1;
});

defineExpose({addParamRow, reactiveData, requestClose, reset, retryParams, show, showEdit, submit});
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
