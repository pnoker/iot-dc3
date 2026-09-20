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
  <div class="alarm-notify">
    <tool-card
      :form-model="searchForm"
      :hide-sort="false"
      :page="state.page"
      @refresh="load"
      @reset="reset"
      @search="search"
      @sort="sort"
      @size-change="sizeChange"
      @current-change="currentChange"
    >
      <template #filters>
        <el-form-item :label="activeConfig.searchLabel" prop="keyword">
          <el-input v-model="searchForm.keyword" :placeholder="activeConfig.searchPlaceholder" clearable/>
        </el-form-item>
        <el-form-item v-if="activeConfig.filterProp" :label="activeConfig.filterLabel" prop="filterValue">
          <enable-flag-segmented
            v-if="activeConfig.filterProp === 'enableFlag'"
            v-model="searchForm.filterValue"
            include-all
          />
          <el-select v-else v-model="searchForm.filterValue" :placeholder="activeConfig.filterPlaceholder" clearable>
            <el-option v-for="option in activeConfig.filterOptions" :key="option.value" v-bind="option"/>
          </el-select>
        </el-form-item>
      </template>
      <template #actions>
        <el-button v-if="activeConfig.editable" :icon="Plus" type="success" @click="openAdd">
          {{ t('common.add') }}
        </el-button>
      </template>
    </tool-card>

    <responsive-record-list
      :columns="columns"
      :empty-text="t('settings.alarm.empty')"
      :error-text="t('common.loadFailed')"
      :loading="state.loading"
      :operation-width="activeConfig.editable ? 180 : 100"
      :rows="state.rows"
      :status="state.status"
      openable
      @open="openDetail"
      @retry="load"
    >
      <template #actions="{row}">
        <el-button link type="primary" @click="openDetail(row)">{{ t('common.detail') }}</el-button>
        <el-button
          v-if="activeConfig.editable"
          :disabled="isDeleting(row)"
          link
          type="primary"
          @click="openEdit(row)"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-popconfirm
          v-if="activeConfig.editable"
          :cancel-button-text="t('common.cancel')"
          :confirm-button-text="t('common.confirm')"
          :title="t('common.confirmDelete', {name: t('common.entityConfig')})"
          @confirm="remove(row)"
        >
          <template #reference>
            <el-button :loading="isDeleting(row)" link type="danger">{{ t('common.delete') }}</el-button>
          </template>
        </el-popconfirm>
      </template>
    </responsive-record-list>

    <el-dialog
      v-model="formVisible"
      :append-to-body="true"
      :before-close="requestCloseForm"
      :close-on-click-modal="!state.saving"
      :close-on-press-escape="!state.saving"
      :show-close="!state.saving"
      :title="dialogTitle"
      class="things-dialog things-dialog--wide"
      destroy-on-close
      draggable
      width="880px"
    >
      <el-alert
        v-if="remoteErrorFields.length"
        :closable="false"
        :title="t('common.optionLoadFailed')"
        class="alarm-notify__alert"
        show-icon
        type="error"
      >
        <el-button :loading="remoteRetrying" link type="danger" @click="retryRemoteOptions">
          {{ t('common.retry') }}
        </el-button>
      </el-alert>
      <el-alert
        v-if="state.saveError"
        :closable="false"
        :title="t('common.saveFailed')"
        class="alarm-notify__alert"
        show-icon
        type="error"
      />
      <el-form :ref="setFormRef" :model="formModel" :rules="formRules" class="alarm-notify__form" label-position="top">
        <!-- Form-column gutter mirrors --dc3-gutter (8px); keep in sync with theme.scss. -->
        <el-row :gutter="8">
          <el-col
            v-for="field in activeConfig.fields"
            :key="field.prop"
            :sm="field.span || 12"
            :xs="24"
          >
            <el-form-item :label="field.label" :prop="field.prop">
              <el-select
                v-if="field.kind === 'select'"
                v-model="formModel[field.prop]"
                :disabled="state.saving"
                :placeholder="field.placeholder"
                clearable
                filterable
              >
                <el-option v-for="option in field.options || []" :key="option.value" v-bind="option"/>
              </el-select>
              <el-select
                v-else-if="field.kind === 'remoteSelect'"
                v-model="formModel[field.prop]"
                :disabled="state.saving"
                :loading="remoteState[field.prop]?.loading"
                :placeholder="field.placeholder"
                clearable
                filterable
                @visible-change="(visible: boolean) => visible && loadRemote(field)"
              >
                <el-option
                  v-for="option in remoteOptions[field.prop] || []"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
              <el-input-number
                v-else-if="field.kind === 'number'"
                v-model="formModel[field.prop]"
                :disabled="state.saving"
                :min="0"
                :precision="field.precision || 0"
                controls-position="right"
                style="width: 100%"
              />
              <enable-flag-segmented
                v-else-if="field.kind === 'enableFlag'"
                v-model="formModel[field.prop]"
                :disabled="state.saving"
              />
              <el-input
                v-else-if="field.kind === 'json' || field.kind === 'textarea'"
                v-model="formModel[field.prop]"
                :autosize="{minRows: field.rows || 4, maxRows: 18}"
                :disabled="state.saving"
                :placeholder="field.placeholder"
                resize="vertical"
                type="textarea"
              />
              <el-input
                v-else
                v-model="formModel[field.prop]"
                :disabled="state.saving"
                :placeholder="field.placeholder"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="things-dialog-footer">
          <el-button :disabled="state.saving" @click="requestCloseForm()">{{ t('common.cancel') }}</el-button>
          <el-button :disabled="state.saving" plain @click="resetForm">{{ t('common.reset') }}</el-button>
          <el-button :loading="state.saving" type="primary" @click="submit">{{ t('common.confirm') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, reactive, watch, type PropType} from 'vue';
import {Plus} from '@element-plus/icons-vue';

import ToolCard from '@/components/card/tool/ToolCard.vue';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';

import type {AlarmFieldConfig, AlarmOption, AlarmTabKey} from './alarmEntityConfig';
import {useAlarmEntityPage} from './useAlarmEntityPage';

const props = defineProps({
  entity: {
    type: String as PropType<AlarmTabKey>,
    required: true,
  },
});

const {
  t,
  formVisible,
  setFormRef,
  formModel,
  searchForm,
  state,
  activeConfig,
  columns,
  dialogTitle,
  formRules,
  load,
  search,
  reset,
  sort,
  sizeChange,
  currentChange,
  openAdd,
  resetForm,
  openEdit,
  requestCloseForm,
  openDetail,
  submit,
  remove,
  isDeleting,
} = useAlarmEntityPage(props);

const remoteOptions = reactive<Record<string, AlarmOption[]>>({});
const remoteState = reactive<Record<string, {loading: boolean; error: boolean}>>({});
const remoteRequestIds: Record<string, number> = {};
let remoteSessionId = 0;

const stateFor = (prop: string) => {
  if (!remoteState[prop]) remoteState[prop] = {loading: false, error: false};
  return remoteState[prop];
};

const loadRemote = async (field: AlarmFieldConfig) => {
  if (!field.loadOptions) return;
  const requestId = (remoteRequestIds[field.prop] || 0) + 1;
  const sessionId = remoteSessionId;
  const optionState = stateFor(field.prop);
  remoteRequestIds[field.prop] = requestId;
  optionState.loading = true;
  try {
    const options = await field.loadOptions({...formModel});
    if (requestId !== remoteRequestIds[field.prop] || sessionId !== remoteSessionId || !formVisible.value) return;
    remoteOptions[field.prop] = options;
    optionState.error = false;
  } catch {
    if (requestId === remoteRequestIds[field.prop] && sessionId === remoteSessionId && formVisible.value) {
      optionState.error = true;
    }
  } finally {
    if (requestId === remoteRequestIds[field.prop] && sessionId === remoteSessionId) optionState.loading = false;
  }
};

const remoteErrorFields = computed(() =>
  activeConfig.value.fields.filter((field) => field.kind === 'remoteSelect' && remoteState[field.prop]?.error)
);
const remoteRetrying = computed(() => remoteErrorFields.value.some((field) => remoteState[field.prop]?.loading));
const retryRemoteOptions = () => {
  remoteErrorFields.value.forEach((field) => void loadRemote(field));
};

watch(formVisible, (visible) => {
  remoteSessionId += 1;
  Object.values(remoteState).forEach((optionState) => {
    optionState.loading = false;
    optionState.error = false;
  });
  if (!visible) return;
  Object.keys(remoteOptions).forEach((key) => delete remoteOptions[key]);
  activeConfig.value.fields.filter((field) => field.kind === 'remoteSelect').forEach((field) => {
    if (formModel[field.prop] != null && formModel[field.prop] !== '') {
      formModel[field.prop] = String(formModel[field.prop]);
    }
    void loadRemote(field);
  });
});

watch(
  () => formModel.alarmTargetTypeFlag,
  (value, previousValue) => {
    if (!formVisible.value || value === previousValue) return;
    formModel.entityId = '';
    const entityField = activeConfig.value.fields.find((field) => field.prop === 'entityId');
    if (entityField) void loadRemote(entityField);
  },
  // `post` batches programmatic form resets: assignForm deletes every key and
  // re-assigns them in one tick, so a same-value reset never fires this
  // watcher (and never clears entityId mid-assign). Only a real net change —
  // the user switching the target type — lands here.
  {flush: 'post'}
);

onBeforeUnmount(() => {
  remoteSessionId += 1;
  Object.keys(remoteRequestIds).forEach((key) => {
    remoteRequestIds[key] = (remoteRequestIds[key] || 0) + 1;
  });
});
</script>

<style lang="scss" scoped>
.alarm-notify {
  min-width: 0;

  &__alert {
    margin-bottom: var(--dc3-space-3);
  }

  &__form {
    :deep(.el-input),
    :deep(.el-select),
    :deep(.el-input-number) {
      width: 100%;
    }
  }
}
</style>
