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
  <el-segmented :model-value="modelValue" :options="options" :size="size" @update:model-value="onChange"/>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';

/**
 * Presets the frontend sends as {@code rangeKey} — kept in sync with
 * backend {@link TimeRangeKeyEnum}. The empty-string sentinel is the
 * "no filter" choice, rendered only when {@code includeAll} is true.
 */
export type RangeKey = '' | 'today' | '24h' | '7d' | '30d';

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
