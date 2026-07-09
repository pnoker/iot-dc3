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
  <div class="tool-card">
    <el-card shadow="never">
      <el-form ref="formRef" :inline="true" :model="formModel" :rules="rules" class="tool-card__body">
        <div class="tool-card-body-form">
          <slot :form-data="formModel" :search="search" name="filters"/>
        </div>
        <!--
          Body-level button row only renders when a caller explicitly overrides
          the `buttons` slot (e.g. PointTool needs pre/next inline with the
          filters). Otherwise the default Search / Reset pair moves down to sit
          next to Add / Import in the footer — every toolbar ends up with one
          button row instead of two stacked rows.
        -->
        <el-form-item v-if="$slots.buttons" class="tool-card-body-button">
          <slot :reset="reset" :search="search" name="buttons"/>
        </el-form-item>
      </el-form>
      <div class="tool-card__footer">
        <div class="tool-card-footer-button">
          <slot name="actions"/>
          <!-- Divider sits between any actions-slot buttons and the default
               Search/Reset pair. It is hidden via CSS (:first-child rule in
               the styles below) when the actions slot renders nothing —
               e.g. DriverTool templates an Add button behind v-if="add"
               which evaluates to false by default. -->
          <span v-if="!$slots.buttons" aria-hidden="true" class="tool-card-footer-divider"/>
          <template v-if="!$slots.buttons">
            <el-button :icon="Search" plain type="primary" @click="search">
              {{ t('common.search') }}
            </el-button>
            <el-button :icon="RefreshRight" plain @click="reset">
              {{ t('common.reset') }}
            </el-button>
          </template>
        </div>
        <div class="tool-card-footer-page">
          <el-pagination
            v-if="!hidePagination"
            :current-page="+page.current"
            :page-size="+page.size"
            :page-sizes="pageSizes"
            :total="+page.total"
            background
            layout="total, prev, pager, next, sizes"
            @size-change="onSizeChange"
            @current-change="onCurrentChange"
          />
          <span aria-hidden="true" class="tool-card-footer-divider"/>
          <el-tooltip :content="t('common.refresh')" effect="dark" placement="top">
            <el-button :icon="Refresh" circle @click="onRefresh"/>
          </el-tooltip>
          <el-tooltip v-if="!hideSort" :content="t('common.sort')" effect="dark" placement="top">
            <el-button :icon="Sort" circle @click="onSort"/>
          </el-tooltip>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {ref, unref} from 'vue';
import {useI18n} from 'vue-i18n';
import type {FormInstance, FormRules} from 'element-plus';
import {Refresh, RefreshRight, Search, Sort} from '@element-plus/icons-vue';

const props = defineProps({
  formModel: {
    type: Object as PropType<Record<string, any>>,
    default: () => ({}),
  },
  rules: {
    type: Object as PropType<FormRules>,
    default: () => ({}),
  },
  page: {
    type: Object as PropType<Record<string, any>>,
    required: true,
  },
  pageSizes: {
    type: Array as PropType<number[]>,
    default: () => [6, 12, 24, 36, 48, 96],
  },
  hideSort: {
    type: Boolean,
    default: false,
  },
  hidePagination: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits<{
  (e: 'search', formData: Record<string, any>): void;
  (e: 'reset'): void;
  (e: 'refresh'): void;
  (e: 'sort'): void;
  (e: 'size-change', size: number): void;
  (e: 'current-change', current: number): void;
}>();

const {t} = useI18n();
const formRef = ref<FormInstance>();

const search = async () => {
  const form = unref(formRef);
  if (!form) {
    emit('search', props.formModel);
    return;
  }

  try {
    await form.validate();
    emit('search', props.formModel);
  } catch {
    // validation errors are displayed by Element Plus
  }
};

const reset = () => {
  const form = unref(formRef);
  form?.resetFields();
  emit('reset');
};

const onRefresh = () => emit('refresh');
const onSort = () => emit('sort');
const onSizeChange = (size: number) => emit('size-change', size);
const onCurrentChange = (current: number) => emit('current-change', current);

defineExpose({search, reset});
</script>

<style lang="scss" scoped>
.tool-card {
  margin: 0 0 4px;

  .tool-card__body {
    display: flex;
    flex-direction: column;
    gap: 8px;

    // Filter row: equal-width wrapping grid. Each el-form-item inside the
    // filters slot occupies 1/4 of the tool card width, so 4 per row by
    // default. Items wrap to the next row once the filter count exceeds
    // four, instead of squeezing into a single horizontal strip.
    .tool-card-body-form {
      display: flex;
      flex-wrap: wrap;
      gap: 8px 12px;

      :deep(.el-form-item) {
        flex: 1 1 calc(25% - 12px);
        min-width: 220px;
        margin: 0;

        .el-form-item__content {
          flex-wrap: nowrap;
        }

        // Force every common input surface to honour the cell width so
        // rows line up. The extra .edit-form-* overrides are here because
        // element-variables.scss pins those helper classes to fixed px
        // widths (used for standalone forms); in the toolbar grid we need
        // them to stretch.
        .el-input,
        .el-input.edit-form-small,
        .el-input.edit-form-medium,
        .el-input.edit-form-default,
        .el-input.edit-form-special,
        .el-input.edit-form-large,
        .el-select,
        .el-select.edit-form-small,
        .el-select.edit-form-medium,
        .el-select.edit-form-default,
        .el-select.edit-form-special,
        .el-select.edit-form-large,
        .el-tree-select,
        .el-date-editor,
        .el-input-number,
        .el-cascader,
        .el-segmented {
          width: 100%;
        }
      }
    }

    .tool-card-body-button {
      width: 100%;
      align-self: center;
      margin: 0;

      :deep(.el-form-item__content) {
        display: flex;
        justify-content: center;
        flex-wrap: wrap;
        gap: 8px;
      }
    }
  }

  .tool-card__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px 12px;
    margin-top: 16px;
    padding-top: 12px;
    border-top: 1px solid var(--el-border-color-lighter);

    .tool-card-footer-button {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .tool-card-footer-page {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 8px;
    }

    // Vertical divider, reused both between actions / search-reset in the
    // left cluster and between pagination / refresh-sort in the right
    // cluster. Keeps the two adjacent button groups visually distinct.
    .tool-card-footer-divider {
      display: inline-block;
      width: 1px;
      height: 18px;
      margin: 0 4px;
      background: var(--el-border-color);
    }

    // If the `actions` slot renders nothing (pages like Driver template an
    // Add button behind v-if="add", so the slot is "used" but the DOM is
    // empty) the divider ends up as the first element and should hide
    // instead of dangling next to nothing. :first-child ignores Vue's
    // comment-node placeholders, so this covers both "no slot supplied"
    // and "slot supplied but all its buttons are v-if'd out".
    .tool-card-footer-button > .tool-card-footer-divider:first-child {
      display: none;
    }
  }

  :deep(.el-card) {
    border: 0;
  }
}
</style>
