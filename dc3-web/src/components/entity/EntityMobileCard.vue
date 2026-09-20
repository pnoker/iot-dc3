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
  <article
    :aria-level="depth + 1"
    :class="{'entity-mobile-card--nested': depth > 0}"
    :style="{'--entity-depth': depth}"
    class="entity-mobile-card"
  >
    <header class="entity-mobile-card__header">
      <span v-if="depth > 0" aria-hidden="true" class="entity-mobile-card__branch">↳</span>
      <!-- Same tone-tile + title anatomy as the driver/device/point cards
           (ThingsCardHeader family) so every card on the platform shares
           one visual grammar. -->
      <span :class="['entity-icon-tile', `entity-icon-tile--${cardTone}`]" aria-hidden="true">
        <el-icon :size="22">
          <component :is="cardIcon"/>
        </el-icon>
      </span>
      <el-button
        v-if="canOpenDetail"
        :aria-label="`${t('common.detail')}: ${primaryText}`"
        class="entity-mobile-card__title-link"
        link
        type="primary"
        @click="openDetail"
      >
        <span class="entity-mobile-card__title" :title="primaryText">{{ primaryText }}</span>
      </el-button>
      <div v-else class="entity-mobile-card__title" :title="primaryText">{{ primaryText }}</div>
      <enable-tag
        v-if="primaryColumn?.kind === 'enable'"
        :value="cellPrimitive(primaryColumn.prop)"
      />
      <el-tag
        v-else-if="primaryColumn?.kind === 'tag'"
        :type="tagTypeValue(primaryColumn.prop)"
      >
        {{ primaryText }}
      </el-tag>
    </header>

    <dl class="entity-mobile-card__fields">
      <div v-for="column in detailColumns" :key="column.prop" class="entity-mobile-card__field">
        <dt>{{ column.label }}</dt>
        <dd :title="formatCell(row, column)">
          <enable-tag v-if="column.kind === 'enable'" :value="cellPrimitive(column.prop)" />
          <el-tag v-else-if="column.kind === 'tag'" :type="tagTypeValue(column.prop)">
            {{ formatCell(row, column) }}
          </el-tag>
          <el-button
            v-else-if="isColumnLinkable(column)"
            class="entity-mobile-card__value-link"
            link
            type="primary"
            @click="column.onClick?.(row)"
          >
            {{ formatCell(row, column) }}
          </el-button>
          <span v-else-if="column.kind === 'color'" class="entity-mobile-card__color">
            <span
              :style="{background: colorValue(column)}"
              class="entity-mobile-card__swatch"
            />
            {{ formatCell(row, column) }}
          </span>
          <span v-else-if="column.kind === 'icon'" class="entity-mobile-card__icon">
            <el-icon v-if="columnIcon(column)"><component :is="columnIcon(column)" /></el-icon>
            {{ formatCell(row, column) }}
          </span>
          <code v-else-if="column.kind === 'code'" class="entity-mobile-card__code">
            {{ formatCell(row, column) }}
          </code>
          <span v-else>{{ formatCell(row, column) }}</span>
        </dd>
      </div>
    </dl>

    <footer v-if="hasActions" class="entity-mobile-card__actions">
      <template v-for="action in config.extraActions || []" :key="action.key">
        <el-popconfirm
          v-if="action.popconfirmTitle"
          :disabled="actionDisabled(action)"
          :cancel-button-text="t('common.cancel')"
          :confirm-button-text="t('common.confirm')"
          :title="action.popconfirmTitle"
          @confirm="onAction(action)"
        >
          <template #reference>
            <el-button
              :disabled="actionDisabled(action)"
              :loading="actionLoading(action)"
              :type="action.type || 'primary'"
              link
            >{{ action.label }}</el-button>
          </template>
        </el-popconfirm>
        <el-button
          v-else
          :disabled="actionDisabled(action)"
          :loading="actionLoading(action)"
          :type="action.type || 'primary'"
          link
          @click="onAction(action)"
        >
          {{ action.label }}
        </el-button>
      </template>
      <el-button
        v-if="config.editable && canEdit(row)"
        :disabled="removing"
        link
        type="primary"
        @click="emit('edit', row)"
      >
        {{ t('common.edit') }}
      </el-button>
      <el-popconfirm
        v-if="config.editable && canDelete(row)"
        :disabled="removing"
        :cancel-button-text="t('common.cancel')"
        :confirm-button-text="t('common.confirm')"
        :title="config.confirmDeleteText || t('common.confirmDelete')"
        @confirm="emit('delete', row)"
      >
        <template #reference>
          <el-button :disabled="removing" :loading="removing" link type="danger">{{ t('common.delete') }}</el-button>
        </template>
      </el-popconfirm>
    </footer>
  </article>
</template>

<script lang="ts" setup>
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';

import EnableTag from '@/components/tag/EnableTag.vue';
import {resolveIcon} from '@/config/constant/icons';
import type {EntityColumnConfig, EntityListConfig, EntityRowAction} from '@/config/types/entityList';

const props = defineProps<{
  config: EntityListConfig;
  row: Record<string, any>;
  formatCell: (row: Record<string, any>, column: EntityColumnConfig) => string;
  getCellValue: (row: Record<string, any>, prop: string) => unknown;
  tagType: (value: unknown) => string;
  canEdit: (row: Record<string, any>) => boolean;
  canDelete: (row: Record<string, any>) => boolean;
  onAction: (action: EntityRowAction, row: Record<string, any>) => void;
  actionLoading: (action: EntityRowAction, row: Record<string, any>) => boolean;
  actionDisabled: (action: EntityRowAction, row: Record<string, any>) => boolean;
  removing?: boolean;
  depth?: number;
}>();

const depth = computed(() => props.depth || 0);

const emit = defineEmits<{
  (event: 'detail', row: Record<string, any>): void;
  (event: 'edit', row: Record<string, any>): void;
  (event: 'delete', row: Record<string, any>): void;
}>();

const {t} = useI18n();

const mobileColumns = computed(() => props.config.columns.filter((column) => column.mobile !== 'hidden'));
const primaryColumn = computed(
  () => mobileColumns.value.find((column) => column.mobile === 'primary') || mobileColumns.value[0]
);
const detailColumns = computed(() => mobileColumns.value.filter((column) => column !== primaryColumn.value));
const primaryText = computed(() =>
  primaryColumn.value ? props.formatCell(props.row, primaryColumn.value) : t('common.empty')
);
const cardIcon = computed(() => props.config.mobileCardIcon || 'Tickets');
const cardTone = computed(() => props.config.mobileCardTone || 'blue');
const canOpenDetail = computed(() => Boolean(props.config.detail));
const hasActions = computed(
  () =>
    Boolean(props.config.editable || (props.config.extraActions?.length ?? 0) > 0)
);

const actionLoading = (action: EntityRowAction) =>
  Boolean(props.actionLoading(action, props.row) || action.loading?.(props.row));
const actionDisabled = (action: EntityRowAction) =>
  Boolean(props.actionDisabled(action, props.row) || action.disabled?.(props.row) || actionLoading(action));
const onAction = (action: EntityRowAction) => props.onAction(action, props.row);

const openDetail = () => {
  if (canOpenDetail.value) emit('detail', props.row);
};

const cellPrimitive = (prop: string): string | number | boolean | null | undefined => {
  const value = props.getCellValue(props.row, prop);
  return ['string', 'number', 'boolean'].includes(typeof value) || value == null
    ? (value as string | number | boolean | null | undefined)
    : String(value);
};

const tagTypeValue = (prop: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const value = props.tagType(props.getCellValue(props.row, prop));
  return ['success', 'primary', 'warning', 'info', 'danger'].includes(value)
    ? (value as 'success' | 'primary' | 'warning' | 'info' | 'danger')
    : 'info';
};

const isColumnLinkable = (column: EntityColumnConfig) =>
  column.kind === 'link' && Boolean(column.onClick) && (column.linkable ? column.linkable(props.row) : true);
const columnIcon = (column: EntityColumnConfig) =>
  column.kind === 'icon' ? resolveIcon(String(props.getCellValue(props.row, column.prop))) : undefined;
const colorValue = (column: EntityColumnConfig) =>
  String(props.getCellValue(props.row, column.prop) || 'var(--el-fill-color-light)');
</script>

<style lang="scss" scoped>
.entity-mobile-card {
  min-width: 0;
  padding: var(--dc3-space-4);
  padding-inline-start: calc(var(--dc3-space-4) + var(--entity-depth, 0) * 12px);
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-lg);
  background: var(--dc3-bg-elevated);
  box-shadow: var(--dc3-shadow-sm);
}

.entity-mobile-card__branch {
  flex: 0 0 auto;
  color: var(--el-color-primary);
  font-size: 16px;
  line-height: var(--dc3-touch-target);
}

.entity-mobile-card__header {
  display: flex;
  align-items: center;
  gap: var(--dc3-space-3);
  min-width: 0;
  // Hairline under the tile header mirrors the ThingsCardHeader anatomy
  // (its enable/disable border) with a neutral rule for generic records.
  padding-bottom: var(--dc3-space-3);
  border-bottom: 1px solid var(--dc3-border-base);
}

.entity-mobile-card__title {
  min-width: 0;
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  color: var(--dc3-text-primary);
  font-size: 15px;
  font-weight: 650;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entity-mobile-card__title-link {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  min-height: var(--dc3-touch-target);
  padding: 0;
  justify-content: flex-start;
  overflow: hidden;

  > :first-child {
    width: 100%;
    min-width: 0;
    max-width: 100%;
  }

  .entity-mobile-card__title {
    color: var(--el-color-primary);
  }
}

.entity-mobile-card__fields {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--dc3-space-2);
  margin: var(--dc3-space-3) 0 0;
}

.entity-mobile-card__field {
  display: grid;
  grid-template-columns: minmax(78px, 34%) minmax(0, 1fr);
  gap: var(--dc3-space-3);
  align-items: baseline;
  min-width: 0;
}

.entity-mobile-card__field dt {
  overflow: hidden;
  color: var(--dc3-text-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entity-mobile-card__field dd {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: var(--dc3-text-regular);
  font-size: 13px;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.entity-mobile-card__value-link {
  max-width: 100%;
  min-height: var(--dc3-touch-target);
  padding: 0;
  justify-content: flex-start;
  text-align: left;
  white-space: normal;
}

.entity-mobile-card__color,
.entity-mobile-card__icon {
  display: inline-flex;
  align-items: center;
  gap: var(--dc3-space-1);
  min-width: 0;
}

.entity-mobile-card__swatch {
  width: 14px;
  height: 14px;
  flex: 0 0 14px;
  border: 1px solid var(--dc3-border-base);
  border-radius: 50%;
}

.entity-mobile-card__code {
  color: inherit;
  background: var(--el-fill-color-light);
  overflow-wrap: anywhere;
  white-space: normal;
}

// Actions dock right like the entity-card footer (ThingsCardActions); on
// phones the buttons stretch edge-to-edge for thumb targets, matching the
// driver/device/point cards exactly.
.entity-mobile-card__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--dc3-space-2);
  margin-top: var(--dc3-space-4);
  padding-top: var(--dc3-space-3);
  box-sizing: border-box;
  border-top: 1px solid var(--dc3-border-base);

  :deep(.el-button) {
    min-height: var(--dc3-touch-target);
    margin: 0;
  }

  @media (max-width: $breakpoint-xs-max) {
    justify-content: stretch;

    :deep(.el-button) {
      flex: 1 1 auto;
    }
  }
}
</style>
