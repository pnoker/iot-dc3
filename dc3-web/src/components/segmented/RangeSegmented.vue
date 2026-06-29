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
  <el-segmented :model-value="modelValue" :options="options" :size="size" @update:model-value="onChange" />
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

  const emit = defineEmits<{(e: 'update:modelValue', value: RangeKey): void}>();

  const {t} = useI18n();

  const options = computed(() => {
    const base: Array<{label: string; value: RangeKey}> = [
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
