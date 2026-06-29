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
  <div class="matrix-toolbar">
    <el-card shadow="never">
      <el-form :inline="true" :model="formModel" class="matrix-toolbar__body">
        <div class="matrix-toolbar-body-form">
          <slot :form-data="formModel" name="filters" />
        </div>
      </el-form>
      <div class="matrix-toolbar__footer">
        <div class="matrix-toolbar-footer-actions">
          <slot name="actions" />
          <span aria-hidden="true" class="matrix-toolbar-footer-divider" />
          <el-button
            :disabled="dirtyCount < 1"
            :icon="Check"
            :loading="saving"
            plain
            type="primary"
            @click="$emit('save')"
          >
            {{ t('device.edit.saveAll') }}
          </el-button>
          <el-popconfirm :title="$t('common.discardConfirm')" @confirm="$emit('discard')">
            <template #reference>
              <el-button :disabled="dirtyCount < 1" :icon="RefreshLeft" plain>
                {{ t('device.edit.discardChanges') }}
              </el-button>
            </template>
          </el-popconfirm>
        </div>
        <div class="matrix-toolbar-footer-trailing">
          <slot name="trailing" />
          <el-tag :type="dirtyCount > 0 ? 'warning' : 'info'" effect="plain">
            {{ t('device.edit.changedCount', {count: dirtyCount}) }}
          </el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import type {PropType} from 'vue';
  import {useI18n} from 'vue-i18n';
  import {Check, RefreshLeft} from '@element-plus/icons-vue';

  defineProps({
    formModel: {
      type: Object as PropType<Record<string, any>>,
      default: () => ({}),
    },
    dirtyCount: {
      type: Number,
      default: 0,
    },
    saving: {
      type: Boolean,
      default: false,
    },
  });

  defineEmits<{
    (e: 'discard'): void;
    (e: 'save'): void;
  }>();

  const {t} = useI18n();
</script>

<style lang="scss" scoped>
  .matrix-toolbar {
    margin: 0 0 4px;

    .matrix-toolbar__body {
      .matrix-toolbar-body-form {
        display: flex;
        flex-wrap: wrap;
        width: 100%;
        gap: 8px 12px;

        // 3-column responsive grid mirroring ToolCard. `flex-grow: 1` lets
        // sparse rows stretch to fill the remaining width.
        :deep(.el-form-item) {
          flex: 1 1 calc(33.333% - 12px);
          min-width: 220px;
          margin: 0;

          .el-form-item__content {
            flex-wrap: nowrap;
          }

          .el-input,
          .el-select,
          .el-tree-select,
          .el-date-editor,
          .el-input-number,
          .el-cascader,
          .el-segmented {
            width: 100%;
          }
        }
      }
    }

    .matrix-toolbar__footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 10px 12px;
      margin-top: 16px;
      padding-top: 12px;
      border-top: 1px solid var(--el-border-color-lighter);

      // Primary actions on the left — mirrors ToolCard's button cluster.
      .matrix-toolbar-footer-actions {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 8px;
      }

      // Status / info on the right — mirrors ToolCard's pagination slot.
      .matrix-toolbar-footer-trailing {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        justify-content: flex-end;
        gap: 8px;
      }

      .matrix-toolbar-footer-divider {
        display: inline-block;
        width: 1px;
        height: 18px;
        margin: 0 4px;
        background: var(--el-border-color);
      }

      .matrix-toolbar-footer-actions > .matrix-toolbar-footer-divider:first-child {
        display: none;
      }
    }

    :deep(.el-card) {
      border: 0;
    }
  }
</style>
