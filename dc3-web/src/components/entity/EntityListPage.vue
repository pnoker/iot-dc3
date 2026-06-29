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
  <div class="entity-list-page">
    <tool-card
      :form-model="searchForm"
      :hide-pagination="config.mode === 'tree'"
      :hide-sort="config.mode === 'tree'"
      :page="config.mode === 'tree' ? {total: 0, size: 0, current: 1} : state.page"
      @refresh="load"
      @reset="reset"
      @search="search"
      @sort="sort"
      @size-change="sizeChange"
      @current-change="currentChange"
    >
      <template #filters="{search: doSearch}">
        <el-form-item v-for="field in config.searchFields" :key="field.prop" :label="field.label" :prop="field.prop">
          <enable-flag-segmented
            v-if="field.kind === 'enableFlag'"
            v-model="searchForm[field.prop]"
            :include-all="field.includeAll"
          />
          <search-segmented
            v-else-if="field.kind === 'select' && !field.multiple && (field.options?.length ?? 0) <= 3"
            v-model="searchForm[field.prop]"
            :options="field.options || []"
          />
          <el-select
            v-else-if="field.kind === 'select'"
            v-model="searchForm[field.prop]"
            :multiple="field.multiple"
            :placeholder="field.placeholder"
            clearable
            collapse-tags
            filterable
          >
            <el-option
              v-for="option in field.options || []"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input
            v-else
            v-model="searchForm[field.prop]"
            :placeholder="field.placeholder"
            clearable
            @keyup.enter="doSearch"
          />
        </el-form-item>
      </template>
      <template #actions>
        <el-button
          v-for="action in config.toolbarActions"
          :key="action.key"
          :icon="resolveIcon(action.icon)"
          :loading="action.loading?.()"
          :type="action.type || 'primary'"
          @click="action.onClick"
        >
          {{ action.label }}
        </el-button>
        <el-button v-if="config.editable" :icon="Plus" type="success" @click="openAdd">
          {{ t('common.add') }}
        </el-button>
      </template>
    </tool-card>

    <blank-card>
      <el-table
        v-loading="state.loading"
        :data="state.rows"
        :default-expand-all="config.mode === 'tree' && config.defaultExpandAll"
        :row-key="config.mode === 'tree' ? config.rowKey || 'id' : undefined"
        class="entity-list-page__table"
        stripe
      >
        <el-table-column
          v-for="column in config.columns"
          :key="column.prop"
          :fixed="column.fixed"
          :label="column.label"
          :min-width="column.minWidth"
          :show-overflow-tooltip="column.overflow !== false"
          :width="column.width"
        >
          <template #default="{row}">
            <enable-tag v-if="column.kind === 'enable'" :value="getCellValue(row, column.prop)" />
            <span v-else-if="column.kind === 'color'" class="entity-list-page__color-cell">
              <span
                :style="{background: getCellValue(row, column.prop) || '#F4F4F5'}"
                class="entity-list-page__swatch"
              />
              {{ formatCell(row, column) }}
            </span>
            <span v-else-if="column.kind === 'icon'" class="entity-list-page__icon-cell">
              <template v-if="cellIcon(row, column.prop)">
                <el-icon><component :is="cellIcon(row, column.prop)" /></el-icon>
              </template>
              {{ getCellValue(row, column.prop) }}
            </span>
            <el-tag v-else-if="column.kind === 'tag'" :type="tagType(getCellValue(row, column.prop))">
              {{ formatCell(row, column) }}
            </el-tag>
            <code v-else-if="column.kind === 'code'" class="entity-list-page__inline-code">
              {{ formatCell(row, column) }}
            </code>
            <template v-else-if="column.kind === 'link'">
              <el-button
                v-if="column.linkable ? column.linkable(row) : true"
                link
                type="primary"
                @click="column.onClick?.(row)"
              >
                {{ formatCell(row, column) }}
              </el-button>
              <span v-else>{{ formatCell(row, column) }}</span>
            </template>
            <span v-else>{{ formatCell(row, column) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="config.detail || config.editable || (config.extraActions && config.extraActions.length)"
          :label="t('common.operation')"
          :width="operationWidth"
          fixed="right"
        >
          <template #default="{row}">
            <el-button v-if="config.detail" link type="primary" @click="openDetail(row)">
              {{ t('common.detail') }}
            </el-button>
            <template v-if="config.extraActions">
              <el-button
                v-for="action in config.extraActions"
                :key="action.key"
                :type="action.type || 'primary'"
                link
                @click="action.onClick(row)"
              >
                {{ action.label }}
              </el-button>
            </template>
            <el-button v-if="config.editable && canEdit(row)" link type="primary" @click="openEdit(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-popconfirm
              v-if="config.editable && canDelete(row)"
              :cancel-button-text="t('common.cancel')"
              :confirm-button-text="t('common.confirm')"
              :title="config.confirmDeleteText || t('common.confirmDelete')"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button link type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="config.emptyText || t('common.empty')" />
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
      :width="config.dialogWidth || '720px'"
      class="things-dialog things-dialog--wide"
      destroy-on-close
      draggable
    >
      <el-form
        :ref="setFormRef"
        :model="formModel"
        :rules="formRules"
        class="entity-list-page__form"
        label-position="top"
      >
        <el-row :gutter="12">
          <el-col v-for="field in config.fields" :key="field.prop" :span="field.span || 12">
            <el-form-item :label="field.label" :prop="field.prop">
              <el-select
                v-if="field.kind === 'select'"
                v-model="formModel[field.prop]"
                :disabled="editing && field.disabledOnEdit"
                :placeholder="field.placeholder"
                clearable
                filterable
              >
                <el-option
                  v-for="option in field.options || []"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
              <el-input-number
                v-else-if="field.kind === 'number'"
                v-model="formModel[field.prop]"
                :disabled="editing && field.disabledOnEdit"
                :min="0"
                :precision="field.precision || 0"
                controls-position="right"
                style="width: 100%"
              />
              <enable-flag-segmented
                v-else-if="field.kind === 'enableFlag'"
                v-model="formModel[field.prop]"
                :disabled="editing && field.disabledOnEdit"
              />
              <el-input
                v-else-if="field.kind === 'json' || field.kind === 'textarea'"
                v-model="formModel[field.prop]"
                :autosize="{minRows: field.rows || 4, maxRows: 18}"
                :disabled="editing && field.disabledOnEdit"
                :placeholder="field.placeholder"
                resize="vertical"
                type="textarea"
              />
              <el-color-picker
                v-else-if="field.kind === 'color'"
                v-model="formModel[field.prop]"
                :disabled="editing && field.disabledOnEdit"
                show-alpha
              />
              <el-tree-select
                v-else-if="field.kind === 'treeSelect'"
                v-model="formModel[field.prop]"
                :check-strictly="field.tree?.checkStrictly"
                :data="treeOptionsFor(field)"
                :disabled="editing && field.disabledOnEdit"
                :node-key="field.tree?.nodeKey || 'id'"
                :props="field.tree?.props"
                clearable
                filterable
              />
              <el-input
                v-else
                v-model="formModel[field.prop]"
                :disabled="editing && field.disabledOnEdit"
                :maxlength="field.maxlength"
                :placeholder="field.placeholder"
                :show-word-limit="!!field.maxlength"
                clearable
              />
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
  import {computed, reactive, watch} from 'vue';
  import {Plus} from '@element-plus/icons-vue';

  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
  import SearchSegmented from '@/components/segmented/SearchSegmented.vue';
  import EnableTag from '@/components/tag/EnableTag.vue';
  import {resolveIcon} from '@/config/constant/icons';
  import type {EntityListConfig} from '@/config/types/entityList';
  import {useEntityListPage} from '@/composables/useEntityListPage';

  const props = defineProps<{config: EntityListConfig}>();

  const {
    t,
    config,
    state,
    searchForm,
    formVisible,
    editing,
    setFormRef,
    formModel,
    formRules,
    dialogTitle,
    load,
    search,
    reset,
    sort,
    sizeChange,
    currentChange,
    openAdd,
    openEdit,
    openDetail,
    resetForm,
    submit,
    remove,
    formatCell,
    tagType,
    canEdit,
    canDelete,
  } = useEntityListPage(props.config);

  // treeSelect: raw rows loaded once on dialog open; transform applied reactively via treeOptionsFor
  const rawTreeData = reactive<Record<string, any[]>>({});
  let treeLoadGen = 0;

  watch(formVisible, async (visible) => {
    if (!visible) return;
    const myGen = ++treeLoadGen;
    for (const field of config.value.fields) {
      if (field.kind === 'treeSelect' && field.tree) {
        const result = await field.tree.load();
        if (myGen === treeLoadGen) {
          rawTreeData[field.prop] = result as any[];
        }
      }
    }
  });

  /** Returns tree options for a treeSelect field, applying transform with the live form model. */
  const treeOptionsFor = (field: {
    prop: string;
    tree?: {transform?: (rows: any[], form: Record<string, any>) => unknown[]};
  }) => {
    const raw = rawTreeData[field.prop] || [];
    return field.tree?.transform ? field.tree.transform(raw, formModel) : raw;
  };

  // Get a nested value by dot-path without going through formatCell
  const getCellValue = (row: Record<string, any>, prop: string): any =>
    prop.split('.').reduce((obj: any, key) => (obj != null ? obj[key] : undefined), row);

  // Resolve icon component once per cell to avoid double call in v-if + :is
  const cellIcon = (row: Record<string, any>, prop: string) => resolveIcon(getCellValue(row, prop));

  // Operation column width based on number of visible action buttons
  const operationWidth = computed(() => {
    if (config.value.operationWidth) return config.value.operationWidth;
    let count = 0;
    if (config.value.detail) count++;
    if (config.value.extraActions) count += config.value.extraActions.length;
    if (config.value.editable) count += 2; // edit + delete
    if (count <= 1) return 100;
    if (count <= 3) return 180;
    if (count === 4) return 260;
    return 320;
  });

  // 供父页在配套弹窗保存 / 工具栏动作完成后刷新表格
  defineExpose({reload: load});
</script>

<style lang="scss" scoped>
  .entity-list-page {
    min-width: 0;

    &__table {
      margin-top: 1px;
      border-radius: 4px;
    }

    &__form {
      :deep(.el-input),
      :deep(.el-select),
      :deep(.el-input-number),
      :deep(.el-tree-select),
      :deep(.el-color-picker) {
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

    &__color-cell {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }

    &__swatch {
      display: inline-block;
      width: 16px;
      height: 16px;
      border-radius: 4px;
      flex-shrink: 0;
    }

    &__icon-cell {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }
</style>
