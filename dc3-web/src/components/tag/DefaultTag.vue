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
  <el-tag :size="size" :type="active ? 'success' : 'info'">
    {{ active ? t('common.yes') : t('common.no') }}
  </el-tag>
</template>

<script lang="ts" setup>
  import {computed} from 'vue';
  import {useI18n} from 'vue-i18n';

  const props = withDefaults(
    defineProps<{
      value?: string | number | boolean | null;
      activeValue?: string | number | boolean;
      size?: '' | 'large' | 'default' | 'small';
    }>(),
    {
      value: undefined,
      activeValue: 'DEFAULT',
      size: '',
    }
  );

  const {t} = useI18n();
  const active = computed(() => props.value === props.activeValue);
</script>
