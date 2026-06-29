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
  <el-drawer
    v-model="detailVisible"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    :title="$t('pointValue.detail.title')"
    direction="rtl"
    size="40%"
  >
    <pre class="json-preview">{{ formatJson(detailData) }}</pre>
  </el-drawer>
</template>

<script lang="ts" setup>
  import type {PropType} from 'vue';
  import {ref} from 'vue';
  import {prettyJson} from '@/utils/jsonUtil';

  defineProps({
    detailData: {
      type: Object as PropType<Record<string, unknown>>,
      default: () => ({}),
    },
  });

  const detailVisible = ref(false);
  const formatJson = (value: unknown) => prettyJson(value);
  const show = () => {
    detailVisible.value = true;
  };

  defineExpose({show});
</script>
