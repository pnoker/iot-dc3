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
  type SegmentedOption = {label: string; value: string | number};

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

  const emit = defineEmits<{(e: 'update:modelValue', value: string | number): void}>();

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
