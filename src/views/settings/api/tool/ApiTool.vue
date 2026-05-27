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
      <el-form-item :label="$t('settings.api.apiName')" prop="apiName">
        <el-input
          v-model="formData.apiName"
          :placeholder="$t('settings.api.apiNamePlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('settings.api.apiCode')" prop="apiCode">
        <el-input
          v-model="formData.apiCode"
          :placeholder="$t('settings.api.apiCodePlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('settings.api.apiGroup')" prop="apiGroup">
        <el-input
          v-model="formData.apiGroup"
          :placeholder="$t('settings.api.apiGroupPlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('settings.api.serviceName')" prop="serviceName">
        <el-input
          v-model="formData.serviceName"
          :placeholder="$t('settings.api.serviceNamePlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('settings.api.apiType')" prop="apiTypeFlag">
        <el-select v-model="formData.apiTypeFlag" :placeholder="$t('common.all')" clearable>
          <el-option label="GET" value="GET" />
          <el-option label="POST" value="POST" />
          <el-option label="PUT" value="PUT" />
          <el-option label="DELETE" value="DELETE" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="formData.enableFlag" include-all />
      </el-form-item>
    </template>
  </tool-card>
</template>

<script lang="ts" setup>
  import { reactive } from 'vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
  import { cleanSearchParams, resetSearchForm } from '@/utils/searchParamUtil';

  defineProps({
    page: {
      type: Object,
      required: true,
    },
  });

  const emit = defineEmits(['search', 'reset', 'refresh', 'sort', 'size-change', 'current-change']);

  const formData = reactive<Record<string, any>>({ enableFlag: '' });

  const onSearch = (data: Record<string, any>) => {
    emit('search', cleanSearchParams(data));
  };

  const onReset = () => {
    resetSearchForm(formData, { enableFlag: '' });
    emit('reset');
  };
</script>
