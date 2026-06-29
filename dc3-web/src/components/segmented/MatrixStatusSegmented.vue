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

  export type MatrixStatus = '' | 'missing' | 'configured' | 'dirty' | 'error';

  defineProps({
    modelValue: {
      type: String as PropType<MatrixStatus>,
      default: '',
    },
    size: {
      type: String as PropType<'' | 'default' | 'small' | 'large'>,
      default: 'default',
    },
  });

  const emit = defineEmits<{(e: 'update:modelValue', value: MatrixStatus): void}>();

  const {t} = useI18n();

  const options = computed<Array<{label: string; value: MatrixStatus}>>(() => [
    {label: t('common.all'), value: ''},
    {label: t('common.configStatus.missing'), value: 'missing'},
    {label: t('common.configStatus.configured'), value: 'configured'},
    {label: t('common.configStatus.dirty'), value: 'dirty'},
    {label: t('common.configStatus.error'), value: 'error'},
  ]);

  const onChange = (value: string | number | boolean) => {
    emit('update:modelValue', value as MatrixStatus);
  };
</script>
