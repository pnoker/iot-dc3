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
  <div class="responsive-record-list">
    <el-alert
      v-if="status === 'error'"
      :closable="false"
      :title="errorText || t('common.loadFailed')"
      class="responsive-record-list__error"
      show-icon
      type="error"
    >
      <el-button :loading="loading" link type="danger" @click="emit('retry')">
        {{ t('common.retry') }}
      </el-button>
    </el-alert>

    <component
      :is="embedded ? 'div' : BlankCard"
      :class="{'responsive-record-list__embedded': embedded}"
    >
      <template v-if="isCardLayout">
        <div
          v-if="loading && rows.length === 0"
          :aria-label="t('common.loading')"
          class="responsive-record-list__skeletons"
          role="status"
        >
          <article v-for="item in 3" :key="item" aria-hidden="true" class="responsive-record-list__skeleton">
            <span class="responsive-record-list__skeleton-line responsive-record-list__skeleton-line--title"/>
            <span class="responsive-record-list__skeleton-line"/>
            <span class="responsive-record-list__skeleton-line responsive-record-list__skeleton-line--short"/>
          </article>
        </div>
        <el-empty
          v-else-if="status === 'error' && rows.length === 0"
          :description="errorText || t('common.loadFailed')"
        />
        <el-empty v-else-if="!loading && rows.length === 0" :description="emptyText || t('common.empty')"/>
        <div v-else :aria-busy="loading" class="responsive-record-list__cards">
          <article
            v-for="row in rows"
            :key="rowKeyValue(row)"
            :class="{'responsive-record-list__card--selected': isSelected(row)}"
            class="responsive-record-list__card"
          >
            <header v-if="primaryColumn" class="responsive-record-list__card-header">
              <el-checkbox
                v-if="selectable"
                :aria-label="`${selectionLabel || t('common.select')}: ${formattedValue(row, primaryColumn)}`"
                :disabled="selectionDisabled?.(row)"
                :model-value="isSelected(row)"
                class="responsive-record-list__selection"
                @change="toggleSelection(row)"
              />
              <el-button
                v-if="openable"
                :aria-label="`${t('common.detail')}: ${formattedValue(row, primaryColumn)}`"
                class="responsive-record-list__title-button"
                link
                type="primary"
                @click="emit('open', row)"
              >
                <span class="responsive-record-list__title">
                  <slot
                    :name="`cell-${primaryColumn.key}`"
                    :column="primaryColumn"
                    :mobile="true"
                    :row="row"
                    :value="cellValue(row, primaryColumn)"
                  >
                    <slot
                      name="cell"
                      :column="primaryColumn"
                      :mobile="true"
                      :row="row"
                      :value="cellValue(row, primaryColumn)"
                    >
                      <responsive-cell :column="primaryColumn" :row="row"/>
                    </slot>
                  </slot>
                </span>
              </el-button>
              <span v-else class="responsive-record-list__title">
                <slot
                  :name="`cell-${primaryColumn.key}`"
                  :column="primaryColumn"
                  :mobile="true"
                  :row="row"
                  :value="cellValue(row, primaryColumn)"
                >
                  <slot
                    name="cell"
                    :column="primaryColumn"
                    :mobile="true"
                    :row="row"
                    :value="cellValue(row, primaryColumn)"
                  >
                    <responsive-cell :column="primaryColumn" :row="row"/>
                  </slot>
                </slot>
              </span>
            </header>

            <dl v-if="primaryColumn" class="responsive-record-list__details">
              <div v-for="column in detailColumns" :key="column.key" class="responsive-record-list__detail">
                <dt>{{ column.label }}</dt>
                <dd
                  :class="{'responsive-record-list__detail--code': column.kind === 'code'}"
                  :title="formattedValue(row, column)"
                >
                  <slot
                    :name="`cell-${column.key}`"
                    :column="column"
                    :mobile="true"
                    :row="row"
                    :value="cellValue(row, column)"
                  >
                    <slot
                      name="cell"
                      :column="column"
                      :mobile="true"
                      :row="row"
                      :value="cellValue(row, column)"
                    >
                      <responsive-cell :column="column" :row="row"/>
                    </slot>
                  </slot>
                </dd>
              </div>
            </dl>

            <footer v-if="hasActions" class="responsive-record-list__actions">
              <slot name="actions" :mobile="true" :row="row"/>
            </footer>
          </article>
        </div>
      </template>

      <el-table
        v-else
        ref="tableRef"
        v-loading="loading"
        :aria-busy="loading"
        :data="rows"
        :row-key="rowKeyForTable"
        class="settings-table"
        stripe
        @selection-change="handleTableSelectionChange"
      >
        <el-table-column
          v-if="selectable"
          :selectable="selectionDisabled ? (row: T) => !selectionDisabled!(row) : undefined"
          reserve-selection
          type="selection"
          width="44"
        />
        <el-table-column
          v-for="column in columns"
          :key="column.key"
          :fixed="column.fixed"
          :label="column.label"
          :min-width="column.minWidth"
          :show-overflow-tooltip="column.overflow !== false"
          :width="column.width"
        >
          <template #default="{row}">
            <slot
              :name="`cell-${column.key}`"
              :column="column"
              :mobile="false"
              :row="row"
              :value="cellValue(row, column)"
            >
              <slot
                name="cell"
                :column="column"
                :mobile="false"
                :row="row"
                :value="cellValue(row, column)"
              >
                <responsive-cell :column="column" :row="row"/>
              </slot>
            </slot>
          </template>
        </el-table-column>
        <el-table-column
          v-if="hasActions"
          :label="t('common.operation')"
          :width="operationWidth"
          fixed="right"
        >
          <template #default="{row}">
            <slot name="actions" :mobile="false" :row="row"/>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty
            :description="status === 'error' ? errorText || t('common.loadFailed') : emptyText || t('common.empty')"
          />
        </template>
      </el-table>
    </component>
  </div>
</template>

<script lang="ts" setup generic="T extends Record<string, any>">
import {computed, defineComponent, h, nextTick, type PropType, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {ElCheckbox, ElTag, type TableInstance} from 'element-plus';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import DefaultTag from '@/components/tag/DefaultTag.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import {useBreakpoint} from '@/composables/useBreakpoint';
import type {ResponsiveListColumn} from '@/config/types';
import {timestampLabel} from '@/utils/dateUtil';

const valueAt = <T extends Record<string, any>>(row: T, prop: string) =>
  prop.split('.').reduce((value: any, key) => (value == null ? undefined : value[key]), row);

const formatValue = <T extends Record<string, any>>(row: T, column?: ResponsiveListColumn<T>): string => {
  if (!column) return '-';
  if (column.formatter) return column.formatter(row);
  const value = valueAt(row, column.prop || column.key);
  if (column.kind === 'time') return timestampLabel(value);
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
};

const ResponsiveCell = defineComponent({
  name: 'ResponsiveCell',
  props: {
    column: {type: Object as PropType<ResponsiveListColumn<any>>, required: true},
    row: {type: Object as PropType<Record<string, any>>, required: true},
  },
  setup(cellProps) {
    return () => {
      const value = valueAt(cellProps.row, cellProps.column.prop || cellProps.column.key);
      const text = formatValue(cellProps.row, cellProps.column);
      if (cellProps.column.kind === 'enable') return h(EnableTag, {value});
      if (cellProps.column.kind === 'default') return h(DefaultTag, {value});
      if (cellProps.column.kind === 'tag') {
        return h(ElTag, {type: cellProps.column.tagType?.(cellProps.row) || 'info'}, () => text);
      }
      if (cellProps.column.kind === 'code') return h('code', {class: 'responsive-record-list__code'}, text);
      return h('span', text);
    };
  },
});

const props = withDefaults(
  defineProps<{
    rows: T[];
    columns: ResponsiveListColumn<T>[];
    loading?: boolean;
    status?: 'idle' | 'loading' | 'success' | 'error';
    rowKey?: string | ((row: T) => string | number);
    emptyText?: string;
    errorText?: string;
    openable?: boolean;
    operationWidth?: number | string;
    embedded?: boolean;
    selectable?: boolean;
    selectedRows?: T[];
    selectionDisabled?: (row: T) => boolean;
    selectionLabel?: string;
  }>(),
  {
    loading: false,
    status: 'idle',
    rowKey: 'id',
    emptyText: '',
    errorText: '',
    openable: false,
    operationWidth: 180,
    embedded: false,
    selectable: false,
    selectedRows: () => [],
    selectionDisabled: undefined,
    selectionLabel: '',
  }
);

const emit = defineEmits<{
  (event: 'retry'): void;
  (event: 'open', row: T): void;
  (event: 'selection-change', rows: T[]): void;
}>();

type SlotProps = {
  column: ResponsiveListColumn<T>;
  mobile: boolean;
  row: T;
  value: unknown;
};

type ResponsiveRecordListSlots = {
  cell?: (props: SlotProps) => unknown;
  actions?: (props: {mobile: boolean; row: T}) => unknown;
} & {
  [slotName: `cell-${string}`]: ((props: SlotProps) => unknown) | undefined;
};

const slots = defineSlots<ResponsiveRecordListSlots>();
const {t} = useI18n();
const {isMobile, isTablet} = useBreakpoint();
const isCardLayout = computed(() => isMobile.value || isTablet.value);
const tableRef = ref<TableInstance>();
let syncingTableSelection = false;

const mobileColumns = computed(() => props.columns.filter((column) => column.mobile !== 'hidden'));
const primaryColumn = computed(
  () => mobileColumns.value.find((column) => column.mobile === 'primary') || mobileColumns.value[0]
);
const detailColumns = computed(() => mobileColumns.value.filter((column) => column !== primaryColumn.value));
const hasActions = computed(() => Boolean(slots.actions));

const cellValue = (row: T, column?: ResponsiveListColumn<T>) =>
  column ? valueAt(row, column.prop || column.key) : undefined;
const formattedValue = (row: T, column?: ResponsiveListColumn<T>) => formatValue(row, column);
const rowKeyValue = (row: T) =>
  typeof props.rowKey === 'function' ? props.rowKey(row) : String(valueAt(row, props.rowKey));
const rowKeyForTable = (row: T) => String(rowKeyValue(row));
const isSelected = (row: T) =>
  props.selectable && props.selectedRows.some((selectedRow) => rowKeyValue(selectedRow) === rowKeyValue(row));
const toggleSelection = (row: T) => {
  if (props.selectionDisabled?.(row)) return;
  const nextRows = isSelected(row)
    ? props.selectedRows.filter((selectedRow) => rowKeyValue(selectedRow) !== rowKeyValue(row))
    : [...props.selectedRows, row];
  emit('selection-change', nextRows);
};
const handleTableSelectionChange = (rows: T[]) => {
  if (!syncingTableSelection) emit('selection-change', rows);
};

watch(
  () => [props.rows, props.selectedRows] as const,
  async () => {
    if (!props.selectable || isCardLayout.value) return;
    await nextTick();
    const table = tableRef.value;
    if (!table) return;
    syncingTableSelection = true;
    table.clearSelection();
    const selectedKeys = new Set(props.selectedRows.map(rowKeyValue));
    props.rows.forEach((row) => {
      if (selectedKeys.has(rowKeyValue(row))) table.toggleRowSelection(row, true);
    });
    syncingTableSelection = false;
  },
  {deep: true}
);
</script>

<style lang="scss" scoped>
.responsive-record-list {
  min-width: 0;
}

.responsive-record-list__embedded {
  box-shadow: none;
  background: transparent;
}

.responsive-record-list__error {
  margin-bottom: var(--dc3-space-3);

  :deep(.el-alert__content) {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--dc3-space-2);
  }
}

.responsive-record-list__cards,
.responsive-record-list__skeletons {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--dc3-space-3);
}

.responsive-record-list__card,
.responsive-record-list__skeleton {
  min-width: 0;
  padding: var(--dc3-space-4);
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-lg);
  background: var(--dc3-bg-elevated);
  box-shadow: var(--dc3-shadow-sm);
}

.responsive-record-list__card--selected {
  border-color: var(--el-color-primary-light-5);
  background: var(--dc3-bg-interactive);
}

.responsive-record-list__card-header {
  display: flex;
  align-items: flex-start;
  gap: var(--dc3-space-2);
  min-width: 0;
}

.responsive-record-list__selection {
  min-width: var(--dc3-touch-target);
  min-height: var(--dc3-touch-target);
  margin: calc(var(--dc3-space-2) * -1) 0;
  justify-content: center;
  flex: 0 0 var(--dc3-touch-target);
}

.responsive-record-list__title-button {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  min-height: var(--dc3-touch-target);
  margin: calc(var(--dc3-space-2) * -1) 0;
  padding: 0;
  justify-content: flex-start;
  overflow: hidden;

  > :first-child {
    width: 100%;
    min-width: 0;
    max-width: 100%;
  }
}

.responsive-record-list__title {
  display: block;
  min-width: 0;
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  color: var(--dc3-text-primary);
  font-size: var(--el-font-size-base);
  font-weight: var(--el-font-weight-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.responsive-record-list__title-button .responsive-record-list__title {
  color: var(--el-color-primary);
}

.responsive-record-list__details {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--dc3-space-2);
  margin: var(--dc3-space-3) 0 0;
}

.responsive-record-list__detail {
  display: grid;
  grid-template-columns: minmax(var(--dc3-list-label-width), 34%) minmax(0, 1fr);
  align-items: baseline;
  gap: var(--dc3-space-3);
  min-width: 0;
}

.responsive-record-list__detail dt,
.responsive-record-list__detail dd {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.responsive-record-list__detail dt {
  color: var(--dc3-text-muted);
  font-size: var(--el-font-size-extra-small);
}

.responsive-record-list__detail dd {
  color: var(--dc3-text-regular);
  font-size: var(--el-font-size-small);
  overflow-wrap: anywhere;
  white-space: normal;
}

.responsive-record-list__detail--code {
  font-family: var(--el-font-family);
}

.responsive-record-list__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--dc3-space-2);
  margin-top: var(--dc3-space-4);
  padding-top: var(--dc3-space-3);
  padding-inline-end: var(--dc3-floating-action-safe-space);
  box-sizing: border-box;
  border-top: 1px solid var(--dc3-border-base);

  :deep(.el-button) {
    min-height: var(--dc3-touch-target);
    margin: 0;
  }
}

.responsive-record-list__skeleton {
  display: grid;
  gap: var(--dc3-space-2);
}

.responsive-record-list__skeleton-line {
  display: block;
  width: 100%;
  height: var(--dc3-skeleton-line-height);
  border-radius: var(--dc3-radius-sm);
  background: var(--el-fill-color-light);
}

.responsive-record-list__skeleton-line--title {
  width: 42%;
  height: var(--dc3-skeleton-title-height);
}

.responsive-record-list__skeleton-line--short {
  width: 68%;
}

:deep(.responsive-record-list__code) {
  color: inherit;
  background: var(--el-fill-color-light);
}
</style>
