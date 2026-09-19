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
  <div :class="['range-segmented', {'range-segmented--select': select}]">
    <el-select
      v-if="select"
      :model-value="modelValue"
      class="range-segmented__select"
      @update:model-value="onChange"
    >
      <el-option v-for="option in options" :key="option.value || 'all'" :label="option.label" :value="option.value" />
    </el-select>
    <el-segmented
      v-else
      :model-value="modelValue"
      :options="options"
      :size="size"
      class="range-segmented__control"
      @update:model-value="onChange"
    />
  </div>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';
import type {RangeKey} from '@/config/types/dashboard';

/**
 * Presets the frontend sends as `rangeKey` — kept in sync with
 * backend {@link TimeRangeKeyEnum}. The empty-string sentinel is the
 * "no filter" choice, rendered only when `includeAll` is true.
 */
export type {RangeKey};

const props = defineProps({
  modelValue: {
    type: String,
    default: '24h',
  },
  /**
   * When true, prepends an "All" choice that maps to the empty string,
   * letting callers opt out of the time filter entirely. Event-page and
   * point-value search bars need this; home-page trend widgets do not.
   */
  includeAll: {
    type: Boolean,
    default: false,
  },
  size: {
    type: String as PropType<'' | 'default' | 'small' | 'large'>,
    default: 'default',
  },
  /** Use a native select when the range is part of a dense filter toolbar. */
  select: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits<{ (e: 'update:modelValue', value: RangeKey): void }>();

const {t} = useI18n();

const options = computed(() => {
  const base: Array<{ label: string; value: RangeKey }> = [
    {label: t('common.ranges.today'), value: 'today'},
    {label: t('common.ranges.h24'), value: '24h'},
    {label: t('common.ranges.d7'), value: '7d'},
    {label: t('common.ranges.d30'), value: '30d'},
  ];
  if (props.includeAll) {
    base.unshift({label: t('common.all'), value: ''});
  }
  return base;
});

const onChange = (value: string | number | boolean) => {
  emit('update:modelValue', value as RangeKey);
};
</script>

<style lang="scss" scoped>
.range-segmented {
  display: block;
  min-width: 0;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
}

.range-segmented--select {
  width: 100%;
  flex: 1 1 140px;
}

.range-segmented__select {
  width: 100%;
  min-width: 120px;
}

.range-segmented__control {
  display: flex;
  width: max-content;
  min-width: 100%;
  white-space: nowrap;

  :deep(.el-segmented__group) {
    width: max-content;
    min-width: 100%;
  }

  :deep(.el-segmented__item) {
    flex: 0 0 auto;
    min-width: max-content;
  }
}

@media (max-width: $breakpoint-xs-max) {
  .range-segmented {
    scrollbar-width: none;
  }

  .range-segmented::-webkit-scrollbar {
    display: none;
  }
}
</style>
