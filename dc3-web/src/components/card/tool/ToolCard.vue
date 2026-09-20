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
            v-if="!hidePagination && !cursorMode"
            :current-page="+page.current"
            :layout="paginationLayout"
            :page-size="+page.size"
            :page-sizes="pageSizes"
            :pager-count="isMobile ? 5 : 7"
            size="default"
            :total="+page.total"
            background
            @size-change="onSizeChange"
            @current-change="onCurrentChange"
          />
          <template v-else-if="cursorMode">
            <el-button :disabled="!cursorPrevious" @click="emit('cursor-previous')">{{ t('common.prevPage') }}</el-button>
            <el-button :disabled="!cursorNext" type="primary" @click="emit('cursor-next')">{{ t('common.nextPage') }}</el-button>
          </template>
          <span aria-hidden="true" class="tool-card-footer-divider"/>
          <!-- Icon-only buttons need explicit accessible names (A7): the
               surrounding tooltip text is not part of the button's name. -->
          <el-tooltip :content="t('common.refresh')" effect="dark" placement="top">
            <el-button :aria-label="t('common.refresh')" :icon="Refresh" circle @click="onRefresh"/>
          </el-tooltip>
          <el-tooltip v-if="!hideSort" :content="t('common.sort')" effect="dark" placement="top">
            <el-button :aria-label="t('common.sort')" :icon="Sort" circle @click="onSort"/>
          </el-tooltip>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed, ref, unref} from 'vue';

import {useBreakpoint} from '@/composables/useBreakpoint';
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
  cursorMode: {
    type: Boolean,
    default: false,
  },
  cursorPrevious: {
    type: Boolean,
    default: false,
  },
  cursorNext: {
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
  (e: 'cursor-previous'): void;
  (e: 'cursor-next'): void;
}>();

const {t} = useI18n();
const {isMobile, isTablet} = useBreakpoint();
const formRef = ref<FormInstance>();

// Pagination degrades to a compact pager on thumb terminals: totals and
// page-size pickers are desktop affordances (A3).
// Tablet toolbars do not have enough horizontal room for the desktop total,
// seven-page pager, and page-size select. Keep the same compact interaction
// used on phones so every navigation control remains visible and tappable.
const paginationLayout = computed(() => (isMobile.value || isTablet.value ? 'prev, pager, next' : 'total, prev, pager, next, sizes'));

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
@use "@/styles/shared-form-widths.scss" as *;

.tool-card {
  margin: 0 0 var(--dc3-gutter);

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

        // Stretch every input surface to the cell width — shared with
        // InfoCard via src/styles/shared-form-widths.scss.
        @include form-item-full-width;

        // Segmented filters keep every option reachable on narrow terminals.
        // Element Plus sizes the group from its labels, so a long enum can
        // otherwise be clipped by the card's overflow boundary. The control
        // becomes its own thumb-scroll surface while the form cell remains
        // fluid.
      }

      :deep(.el-form-item .el-segmented) {
        min-width: 0;
        max-width: 100%;
        overflow-x: auto;
        overflow-y: hidden;
        -webkit-overflow-scrolling: touch;
      }

      :deep(.el-form-item .el-segmented__group) {
        width: max-content;
        min-width: 100%;
      }

      :deep(.el-form-item .el-segmented__item) {
        flex: 0 0 auto;
        min-width: max-content;
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
    @include card-footer;

    // The two footer clusters split space by content, not 50/50: the
    // button row takes exactly what its buttons need and the pager cluster
    // stretches over the rest. A fixed 1 1 280px basis on both sides
    // starved wider button rows (Device = Add + Import + Search + Reset)
    // and wrapped the last button even when the whole footer row had room.
    .tool-card-footer-button {
      display: flex;
      align-items: center;
      flex: 0 1 auto;
      min-width: 0;
      max-width: 100%;
      flex-wrap: nowrap;
      gap: 8px;
      // Never break buttons across rows — on a truly narrow viewport the
      // row scrolls horizontally instead (same pattern as the segmented
      // filters above).
      overflow-x: auto;
      overflow-y: hidden;
      -webkit-overflow-scrolling: touch;

      .tool-card-footer-divider,
      :deep(.el-button) {
        flex-shrink: 0;
      }
    }

    // Pagination + refresh/sort stay on one line at every tier: the pager
    // shrinks and scrolls internally instead of pushing the icon tools onto
    // a second row (a wrapped row wastes vertical space and reads badly on
    // narrow terminals).
    .tool-card-footer-page {
      display: flex;
      align-items: center;
      flex: 1 1 auto;
      min-width: 0;
      max-width: 100%;
      flex-wrap: nowrap;
      justify-content: flex-end;
      gap: 8px;

      :deep(.el-pagination) {
        min-width: 0;
        max-width: 100%;
        overflow-x: auto;
        overflow-y: hidden;
      }

      // The divider and the round icon tools never shrink away — only the
      // pager yields.
      .tool-card-footer-divider,
      :deep(.el-button) {
        flex-shrink: 0;
      }
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

    @media (max-width: $breakpoint-xs-max) {
      align-items: stretch;

      .tool-card-footer-button,
      .tool-card-footer-page {
        width: 100%;
        flex: 0 1 100%;
        justify-content: center;
      }

      // Phone layout: the whole cluster (pager + divider + refresh/sort)
      // centers as one group — same axis as the button row above, so the
      // two footer rows read as one aligned block. Still one line: only
      // the pager scrolls internally when the viewport cannot fit both at
      // full size.
      .tool-card-footer-page {
        :deep(.el-pagination) {
          flex: 0 1 auto;
          min-width: 0;
          overflow-x: auto;
        }
      }
    }
  }

  :deep(.el-card) {
    border-color: var(--dc3-border-base);
  }

  // Match the CardShell family padding (Element Plus defaults to 20px).
  :deep(.el-card__body) {
    padding: var(--dc3-space-4);
  }
}
</style>
