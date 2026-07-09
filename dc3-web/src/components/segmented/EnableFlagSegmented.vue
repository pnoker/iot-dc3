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
  <el-segmented
    :model-value="modelValue"
    :options="options"
    :size="size"
    class="enable-flag-segmented"
    @update:model-value="onChange"
  />
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';

type ValueType = 'flag' | 'number';
type SegmentedOption = { label: string; value: string | number };

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: 'ENABLE',
  },
  includeAll: {
    type: Boolean,
    default: false,
  },
  valueType: {
    type: String as PropType<ValueType>,
    default: 'flag',
  },
  size: {
    type: String as PropType<'' | 'default' | 'small' | 'large'>,
    default: 'default',
  },
});

const emit = defineEmits<{ (e: 'update:modelValue', value: string | number): void }>();

const {t} = useI18n();

const options = computed<SegmentedOption[]>(() => {
  const base =
    props.valueType === 'number'
      ? [
        {label: t('common.enable'), value: 0},
        {label: t('common.disable'), value: 1},
      ]
      : [
        {label: t('common.enable'), value: 'ENABLE'},
        {label: t('common.disable'), value: 'DISABLE'},
      ];
  if (props.includeAll) {
    return [{label: t('common.all'), value: ''}, ...base];
  }
  return base;
});

const onChange = (value: string | number | boolean) => {
  if (props.includeAll && value === '') {
    emit('update:modelValue', '');
    return;
  }
  if (props.valueType === 'number') {
    emit('update:modelValue', Number(value) === 1 ? 1 : 0);
    return;
  }
  emit('update:modelValue', value === 'DISABLE' ? 'DISABLE' : 'ENABLE');
};
</script>

<style lang="scss" scoped>
.enable-flag-segmented {
  width: 100%;

  :deep(.el-segmented__item) {
    flex: 1 1 0;
  }
}
</style>
