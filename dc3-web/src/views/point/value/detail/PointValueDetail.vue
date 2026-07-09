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
