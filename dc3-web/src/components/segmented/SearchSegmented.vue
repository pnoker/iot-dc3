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
    :model-value="normalizedValue"
    :options="allOptions"
    class="search-segmented"
    @update:model-value="onChange"
  />
</template>

<script lang="ts" setup>
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';

import type {EntityOption} from '@/config/types/entityList';

// Single-select search filter rendered as a segmented control. The project
// convention is: a single-select filter with ≤ 3 options uses Segmented;
// more than 3 falls back to el-select. A search filter must be clearable, but
// el-segmented always has a selection — so we prepend an explicit "all" choice
// (value '') that maps back to "no filter", mirroring EnableFlagSegmented.
const props = defineProps<{
  modelValue: string | number;
  options: EntityOption[];
}>();

const emit = defineEmits<{ (e: 'update:modelValue', value: string | number): void }>();

const {t} = useI18n();

const allOptions = computed<EntityOption[]>(() => [{label: t('common.all'), value: ''}, ...props.options]);

const normalizedValue = computed(() => props.modelValue ?? '');

const onChange = (value: string | number | boolean) => {
  emit('update:modelValue', value as string | number);
};
</script>

<style lang="scss" scoped>
.search-segmented {
  width: 100%;

  :deep(.el-segmented__item) {
    flex: 1 1 0;
  }
}
</style>
