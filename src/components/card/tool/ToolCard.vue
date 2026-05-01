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
  <div class="tool-card">
    <el-card shadow="never">
      <el-form ref="formRef" :inline="true" :model="formModel" :rules="rules" class="tool-card__body">
        <div class="tool-card-body-form">
          <slot name="filters" :form-data="formModel" :search="search" />
        </div>
        <el-form-item class="tool-card-body-button">
          <slot name="buttons" :search="search" :reset="reset">
            <el-button :icon="Search" type="primary" @click="search">
              {{ t('common.search') }}
            </el-button>
            <el-button :icon="RefreshRight" @click="reset">
              {{ t('common.reset') }}
            </el-button>
          </slot>
        </el-form-item>
      </el-form>
      <div class="tool-card__footer">
        <div class="tool-card-footer-button">
          <slot name="actions" />
        </div>
        <div class="tool-card-footer-page">
          <el-pagination
            :current-page="+page.current"
            :page-size="+page.size"
            :page-sizes="pageSizes"
            :total="+page.total"
            background
            layout="total, prev, pager, next, sizes"
            @size-change="onSizeChange"
            @current-change="onCurrentChange"
          />
          <el-tooltip :content="t('common.refresh')" effect="dark" placement="top">
            <el-button :icon="Refresh" circle @click="onRefresh" />
          </el-tooltip>
          <el-tooltip v-if="!hideSort" :content="t('common.sort')" effect="dark" placement="top">
            <el-button :icon="Sort" circle @click="onSort" />
          </el-tooltip>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import { ref, unref } from 'vue';
  import type { PropType } from 'vue';
  import { useI18n } from 'vue-i18n';
  import type { FormInstance, FormRules } from 'element-plus';
  import { Refresh, RefreshRight, Search, Sort } from '@element-plus/icons-vue';

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
  });

  const emit = defineEmits<{
    (e: 'search', formData: Record<string, any>): void;
    (e: 'reset'): void;
    (e: 'refresh'): void;
    (e: 'sort'): void;
    (e: 'size-change', size: number): void;
    (e: 'current-change', current: number): void;
  }>();

  const { t } = useI18n();
  const formRef = ref<FormInstance>();

  const search = () => {
    const form = unref(formRef);
    form?.validate((valid) => {
      if (valid) emit('search', props.formModel);
    });
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

  defineExpose({ search, reset });
</script>

<style lang="scss" scoped>
  .tool-card {
    margin: 1px 0 4px;

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
        align-self: flex-end;
        margin: 0;

        :deep(.el-form-item__content) {
          display: flex;
          gap: 8px;
        }
      }
    }

    .tool-card__footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 8px;

      .tool-card-footer-button {
        display: flex;
        gap: 8px;
      }

      .tool-card-footer-page {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }

    :deep(.el-card) {
      border: 0;
    }
  }
</style>
