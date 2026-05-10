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
  <tool-card
    :form-model="formData"
    :rules="formRule"
    :page="page"
    @search="onSearch"
    @reset="onReset"
    @refresh="$emit('refresh')"
    @sort="$emit('sort')"
    @size-change="$emit('size-change', $event)"
    @current-change="$emit('current-change', $event)"
  >
    <template #filters>
      <el-form-item :label="$t('driver.tool.driverName')" prop="driverName">
        <el-input
          v-model="formData.driverName"
          class="edit-form-default"
          clearable
          :placeholder="$t('driver.tool.driverNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('driver.tool.serviceName')" prop="serviceName">
        <el-input
          v-model="formData.serviceName"
          class="edit-form-default"
          clearable
          :placeholder="$t('driver.tool.serviceNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('driver.tool.host')" prop="serviceHost">
        <el-input
          v-model="formData.serviceHost"
          class="edit-form-default"
          clearable
          :placeholder="$t('driver.tool.hostPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <el-segmented
          v-model="formData.enableFlag"
          :options="[
            { label: $t('common.all'), value: '' },
            { label: $t('common.enable'), value: 'ENABLE' },
            { label: $t('common.disable'), value: 'DISABLE' },
          ]"
        />
      </el-form-item>
    </template>
    <template #actions>
      <el-button v-if="add" :icon="Plus" type="success">{{ $t('common.add') }}</el-button>
    </template>
  </tool-card>
</template>

<script lang="ts" setup>
  import { reactive } from 'vue';
  import type { FormRules } from 'element-plus';
  import { Plus } from '@element-plus/icons-vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import { cleanSearchParams, resetSearchForm } from '@/utils/searchParamUtil';

  defineProps({
    page: {
      type: Object,
      required: true,
    },
    add: {
      type: Boolean,
      default: false,
    },
  });

  const emit = defineEmits(['search', 'reset', 'refresh', 'sort', 'size-change', 'current-change']);

  const formData = reactive<Record<string, any>>({ enableFlag: '' });
  const formRule = reactive<FormRules>({
    port: [{ type: 'number', message: 'Port must be a number' }],
  });

  const onSearch = (data: Record<string, any>) => {
    emit('search', cleanSearchParams(data));
  };

  const onReset = () => {
    resetSearchForm(formData, { enableFlag: '' });
    emit('reset');
  };
</script>
