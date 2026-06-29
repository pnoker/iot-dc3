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
          <el-input v-model="searchForm.keyword" :placeholder="activeConfig.searchPlaceholder" clearable />
        </el-form-item>
        <el-form-item v-if="activeConfig.filterProp" :label="activeConfig.filterLabel" prop="filterValue">
          <enable-flag-segmented
            v-if="activeConfig.filterProp === 'enableFlag'"
            v-model="searchForm.filterValue"
            include-all
          />
          <el-select v-else v-model="searchForm.filterValue" :placeholder="activeConfig.filterPlaceholder" clearable>
            <el-option v-for="option in activeConfig.filterOptions" :key="option.value" v-bind="option" />
          </el-select>
        </el-form-item>
      </template>
      <template #actions>
        <el-button v-if="activeConfig.editable" :icon="Plus" type="success" @click="openAdd">
          {{ t('common.add') }}
        </el-button>
      </template>
    </tool-card>

    <blank-card>
      <el-table v-loading="state.loading" :data="state.rows" class="alarm-notify__table" stripe>
        <!-- @vue-generic {import('@/config/types').AlarmEntity} -->
        <el-table-column
          v-for="column in activeConfig.columns"
          :key="column.prop"
          :fixed="column.fixed"
          :label="column.label"
          :min-width="column.minWidth"
          :prop="column.prop"
          :show-overflow-tooltip="column.overflow !== false"
          :width="column.width"
        >
          <template #default="{row}">
            <el-tag v-if="column.kind === 'tag'" :type="tagType(row[column.prop], column.prop)">
              {{ formatCell(row, column) }}
            </el-tag>
            <code v-else-if="column.kind === 'code'" class="alarm-notify__inline-code">
              {{ formatCell(row, column) }}
            </code>
            <span v-else>{{ formatCell(row, column) }}</span>
          </template>
        </el-table-column>
        <!-- @vue-generic {import('@/config/types').AlarmEntity} -->
        <el-table-column :label="t('common.operation')" :width="activeConfig.editable ? 180 : 100" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" @click="openDetail(row)">{{ t('common.detail') }}</el-button>
            <el-button v-if="activeConfig.editable" link type="primary" @click="openEdit(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-popconfirm
              v-if="activeConfig.editable"
              :cancel-button-text="t('common.cancel')"
              :confirm-button-text="t('common.confirm')"
              :title="t('settings.alarm.confirmDelete')"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button link type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.alarm.empty')" />
        </template>
      </el-table>
    </blank-card>

    <el-dialog
      v-model="formVisible"
      :append-to-body="true"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      :title="dialogTitle"
      class="things-dialog things-dialog--wide"
      destroy-on-close
      draggable
      width="880px"
    >
      <el-form :ref="setFormRef" :model="formModel" :rules="formRules" class="alarm-notify__form" label-position="top">
        <el-row :gutter="12">
          <el-col v-for="field in activeConfig.fields" :key="field.prop" :span="field.span || 12">
            <el-form-item :label="field.label" :prop="field.prop">
              <el-select
                v-if="field.kind === 'select'"
                v-model="formModel[field.prop]"
                :placeholder="field.placeholder"
                clearable
                filterable
              >
                <el-option v-for="option in field.options || []" :key="option.value" v-bind="option" />
              </el-select>
              <el-select
                v-else-if="field.kind === 'remoteSelect'"
                v-model="formModel[field.prop]"
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
                :min="0"
                :precision="field.precision || 0"
                controls-position="right"
                style="width: 100%"
              />
              <enable-flag-segmented v-else-if="field.kind === 'enableFlag'" v-model="formModel[field.prop]" />
              <el-input
                v-else-if="field.kind === 'json' || field.kind === 'textarea'"
                v-model="formModel[field.prop]"
                :autosize="{minRows: field.rows || 4, maxRows: 18}"
                :placeholder="field.placeholder"
                resize="vertical"
                type="textarea"
              />
              <el-input v-else v-model="formModel[field.prop]" :placeholder="field.placeholder" clearable />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="things-dialog-footer">
          <el-button @click="formVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button plain @click="resetForm">{{ t('common.reset') }}</el-button>
          <el-button :loading="state.saving" type="primary" @click="submit">{{ t('common.confirm') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
  import {reactive, watch} from 'vue';
  import {Plus} from '@element-plus/icons-vue';

  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';

  import type {AlarmFieldConfig, AlarmOption} from './alarmEntityConfig';
  import {type AlarmEntityPageProps, useAlarmEntityPage} from './useAlarmEntityPage';

  const props = defineProps<AlarmEntityPageProps>();

  const {
    t,
    formVisible,
    setFormRef,
    formModel,
    searchForm,
    state,
    activeConfig,
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
    openDetail,
    submit,
    remove,
    tagType,
    formatCell,
  } = useAlarmEntityPage(props);

  // remoteSelect option cache keyed by field.prop. Loaded when the dialog opens
  // (so edit-mode values render as names) and on each dropdown expand (so entityId
  // reflects the currently selected alarmTargetTypeFlag).
  const remoteOptions = reactive<Record<string, AlarmOption[]>>({});
  const loadRemote = async (field: AlarmFieldConfig) => {
    if (!field.loadOptions) return;
    remoteOptions[field.prop] = await field.loadOptions(formModel);
  };
  watch(formVisible, (visible) => {
    if (!visible) return;
    activeConfig.value.fields
      .filter((field) => field.kind === 'remoteSelect')
      .forEach((field) => {
        if (formModel[field.prop] != null && formModel[field.prop] !== '') {
          formModel[field.prop] = String(formModel[field.prop]);
        }
        loadRemote(field);
      });
  });
</script>

<style lang="scss" scoped>
  .alarm-notify {
    min-width: 0;

    &__table {
      margin-top: 1px;
      border-radius: 4px;
    }

    &__form {
      :deep(.el-input),
      :deep(.el-select),
      :deep(.el-input-number) {
        width: 100%;
      }
    }

    &__inline-code {
      padding: 2px 5px;
      border-radius: 4px;
      color: var(--el-text-color-regular);
      background: var(--el-fill-color-light);
      font-size: 12px;
    }
  }
</style>
