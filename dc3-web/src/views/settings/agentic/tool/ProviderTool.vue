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
    :page="page"
    @refresh="$emit('refresh')"
    @reset="onReset"
    @search="onSearch"
    @sort="$emit('sort')"
    @size-change="$emit('size-change', $event)"
    @current-change="$emit('current-change', $event)"
  >
    <template #filters>
      <el-form-item :label="$t('settings.agentic.providerName')" prop="name">
        <el-input v-model="formData.name" :placeholder="$t('settings.agentic.providerNamePlaceholder')" clearable />
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.providerType')" prop="providerType">
        <el-select
          v-model="formData.providerType"
          :placeholder="$t('settings.agentic.providerTypeAllPlaceholder')"
          clearable
          style="width: 100%"
        >
          <el-option v-for="pt in providerTypes" :key="pt.value" :label="pt.label" :value="pt.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="formData.enableFlag" include-all />
      </el-form-item>
    </template>
    <template #actions>
      <el-button :icon="Plus" type="success" @click="$emit('add')">
        {{ $t('common.add') }}
      </el-button>
    </template>
  </tool-card>
</template>

<script lang="ts" setup>
  import {reactive} from 'vue';
  import {Plus} from '@element-plus/icons-vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
  import {cleanSearchParams, resetSearchForm} from '@/utils/searchParamUtil';

  import {AGENTIC_PROVIDER_TYPES} from '../providerTypes';

  const providerTypes = AGENTIC_PROVIDER_TYPES;

  defineProps<{
    page: Record<string, any>;
  }>();

  const emit = defineEmits<{
    (e: 'search', data: Record<string, any>): void;
    (e: 'reset'): void;
    (e: 'refresh'): void;
    (e: 'sort'): void;
    (e: 'add'): void;
    (e: 'size-change', size: number): void;
    (e: 'current-change', current: number): void;
  }>();

  const formData = reactive<Record<string, any>>({enableFlag: ''});

  const onSearch = (data: Record<string, any>) => {
    emit('search', cleanSearchParams(data));
  };

  const onReset = () => {
    resetSearchForm(formData, {enableFlag: ''});
    emit('reset');
  };
</script>
