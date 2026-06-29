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

  const emit = defineEmits<{(e: 'update:modelValue', value: string | number): void}>();

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
