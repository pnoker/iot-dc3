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

const emit = defineEmits<{ (e: 'update:modelValue', value: MatrixStatus): void }>();

const {t} = useI18n();

const options = computed<Array<{ label: string; value: MatrixStatus }>>(() => [
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
