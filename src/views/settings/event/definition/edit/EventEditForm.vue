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
    :title="reactiveData.mode === 'add' ? $t('eventDefinition.form.addTitle') : $t('eventDefinition.form.editTitle')"
    class="things-dialog"
    draggable
    @closed="reset"
  >
    <el-form ref="formRef" :model="reactiveData.form" :rules="rules" class="things-form-grid" label-position="top">
      <el-form-item :label="$t('common.name')" prop="eventName">
        <el-input v-model="reactiveData.form.eventName" :placeholder="$t('common.name')" clearable />
      </el-form-item>
      <el-form-item :label="$t('eventDefinition.form.code')" prop="eventCode">
        <el-input v-model="reactiveData.form.eventCode" :placeholder="$t('eventDefinition.form.code')" clearable />
      </el-form-item>
      <el-form-item :label="$t('eventDefinition.form.eventType')" prop="eventTypeFlag">
        <el-select v-model="reactiveData.form.eventTypeFlag" clearable>
          <el-option v-for="opt in EVENT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('eventDefinition.form.eventLevel')" prop="eventLevelFlag">
        <el-select v-model="reactiveData.form.eventLevelFlag" clearable>
          <el-option v-for="opt in EVENT_LEVEL_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
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
        <span>{{ $t('eventDefinition.form.params') }}</span>
        <el-button :icon="Plus" size="small" type="success" @click="addParamRow">
          {{ $t('common.add') }}
        </el-button>
      </div>
      <el-table :data="reactiveData.params" border max-height="260" size="small">
        <el-table-column :label="$t('common.name')" min-width="170">
          <template #default="{ row }">
            <el-input v-model="row.paramName" clearable />
          </template>
        </el-table-column>
        <el-table-column :label="$t('eventDefinition.form.code')" min-width="170">
          <template #default="{ row }">
            <el-input v-model="row.paramCode" clearable />
          </template>
        </el-table-column>
        <el-table-column :label="$t('eventDefinition.form.type')" min-width="140">
          <template #default="{ row }">
            <el-select v-model="row.paramTypeFlag">
              <el-option v-for="opt in POINT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="$t('eventDefinition.form.enabled')" width="104">
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
  import { listEventParamByEventId } from '@/api/event';
  import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
  import { EVENT_LEVEL_OPTIONS, EVENT_TYPE_OPTIONS, POINT_TYPE_OPTIONS } from '@/config/constant/enums';
  import type { EventForm, EventParamRecord, EventRecord } from '@/config/types';
  import { failMessage } from '@/utils/notificationUtil';
  import { enableFlagValue, eventLevelValue, eventTypeValue, pointTypeValue } from '@/utils/thingModelFormatUtil';

  type FormMode = 'add' | 'edit';
  type DoneCallback = (close?: boolean) => void;

  const { t } = useI18n();

  type EventParamDraft = EventParamRecord & { _key: string };

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
    eventName: [{ required: true, message: t('eventDefinition.form.nameRequired'), trigger: 'blur' }],
    eventCode: [{ required: true, message: t('eventDefinition.form.codeRequired'), trigger: 'blur' }],
  };

  const reset = () => {
    reactiveData.form = { ...reactiveData.originalForm };
    reactiveData.params = cloneParams(reactiveData.originalParams);
    reactiveData.submitting = false;
    formRef.value?.clearValidate();
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
    reactiveData.params
      .filter((item) => String(item.paramName || item.paramCode || '').trim() !== '')
      .map((item) => {
        const param = { ...item } as EventParamRecord;
        delete (param as { _key?: string })._key;
        return {
          ...param,
          paramName: String(item.paramName || '').trim(),
          paramCode: String(item.paramCode || '').trim(),
          paramTypeFlag: item.paramTypeFlag || 'STRING',
          enableFlag: item.enableFlag || 'ENABLE',
        };
      });

  const validateParams = (params: EventParamRecord[]) => {
    const codes = new Set<string>();
    for (const item of params) {
      const code = String(item.paramCode || '').trim();
      if (!item.paramName || !code || !item.paramTypeFlag) {
        failMessage(t('eventDefinition.form.paramRequired'));
        return false;
      }
      if (codes.has(code)) {
        failMessage(t('eventDefinition.form.paramCodeUnique'));
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
    reactiveData.originalForm = { ...initial };
    reactiveData.form = { ...initial };
    reactiveData.originalParams = [];
    reactiveData.params = [];
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
    const valid = await formRef.value?.validate().catch(() => false);
    if (!valid) return;
    reactiveData.submitting = true;
    const payload = { ...reactiveData.form };
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
