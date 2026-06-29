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
  <div class="info-card">
    <el-card shadow="never">
      <el-form
        ref="formRef"
        :inline="true"
        :model="formModel"
        :rules="rules"
        class="info-card__body"
        label-position="top"
      >
        <div class="info-card-body-form">
          <slot :form-data="formModel" name="fields" />
        </div>
      </el-form>
      <div class="info-card__footer">
        <div class="info-card-footer-actions">
          <slot name="actions" />
          <span v-if="!hideDefaultActions" aria-hidden="true" class="info-card-footer-divider" />
          <template v-if="!hideDefaultActions">
            <el-button :icon="Check" plain type="primary" @click="onSave">
              {{ t('common.save') }}
            </el-button>
            <el-button :icon="RefreshLeft" plain @click="onReset">
              {{ t('common.reset') }}
            </el-button>
          </template>
        </div>
        <div class="info-card-footer-trailing">
          <slot name="trailing" />
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
  import {Check, RefreshLeft} from '@element-plus/icons-vue';

  defineProps({
    formModel: {
      type: Object as PropType<Record<string, any>>,
      default: () => ({}),
    },
    rules: {
      type: Object as PropType<FormRules>,
      default: () => ({}),
    },
    hideDefaultActions: {
      type: Boolean,
      default: false,
    },
  });

  const emit = defineEmits<{
    (e: 'save'): void;
    (e: 'reset'): void;
  }>();

  const {t} = useI18n();
  const formRef = ref<FormInstance>();

  const onSave = async () => {
    const form = unref(formRef);
    if (!form) {
      emit('save');
      return;
    }
    try {
      await form.validate();
      emit('save');
    } catch {
      // validation errors are displayed by Element Plus
    }
  };

  const onReset = () => {
    const form = unref(formRef);
    form?.clearValidate();
    emit('reset');
  };

  defineExpose({formRef});
</script>

<style lang="scss" scoped>
  .info-card {
    margin: 0 0 4px;

    .info-card__body {
      .info-card-body-form {
        display: flex;
        flex-wrap: wrap;
        width: 100%;
        gap: 8px 12px;

        // 3-column responsive grid mirroring ToolCard's 4-column logic.
        // `flex-grow: 1` lets a row with fewer items stretch to fill the
        // remaining width (2 items → 50% each, 1 item alone → 100%).
        :deep(.el-form-item) {
          flex: 1 1 calc(33.333% - 12px);
          min-width: 220px;
          margin: 0;

          .el-form-item__content {
            flex-wrap: nowrap;
          }

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

        :deep(.el-form-item.info-card-item-full) {
          flex: 1 1 100%;
        }

        :deep(.el-form-item.info-card-item-half) {
          flex: 1 1 calc(50% - 12px);
        }
      }
    }

    .info-card__footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 10px 12px;
      margin-top: 16px;
      padding-top: 12px;
      border-top: 1px solid var(--el-border-color-lighter);

      // Primary actions cluster on the left — mirrors ToolCard.
      .info-card-footer-actions {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 8px;
      }

      // Secondary info / extras on the right — mirrors ToolCard's pagination
      // slot. Empty by default; pages opt-in with the trailing slot.
      .info-card-footer-trailing {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        justify-content: flex-end;
        gap: 8px;
      }

      .info-card-footer-divider {
        display: inline-block;
        width: 1px;
        height: 18px;
        margin: 0 4px;
        background: var(--el-border-color);
      }

      .info-card-footer-actions > .info-card-footer-divider:first-child {
        display: none;
      }
    }

    :deep(.el-card) {
      border: 0;
    }
  }
</style>
