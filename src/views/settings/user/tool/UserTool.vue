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
      <el-form-item :label="$t('settings.user.nickName')" prop="nickName">
        <el-input
          v-model="formData.nickName"
          :placeholder="$t('settings.user.nickNamePlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('settings.user.userName')" prop="userName">
        <el-input
          v-model="formData.userName"
          :placeholder="$t('settings.user.userNamePlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('settings.user.phone')" prop="phone">
        <el-input
          v-model="formData.phone"
          :placeholder="$t('settings.user.phonePlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('settings.user.email')" prop="email">
        <el-input
          v-model="formData.email"
          :placeholder="$t('settings.user.emailPlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="formData.enableFlag" include-all value-type="number" />
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
  import { reactive } from 'vue';
  import { Plus } from '@element-plus/icons-vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
  import { cleanSearchParams, resetSearchForm } from '@/utils/searchParamUtil';

  defineProps({
    page: {
      type: Object,
      required: true,
    },
  });

  const emit = defineEmits(['search', 'reset', 'refresh', 'sort', 'add', 'size-change', 'current-change']);

  const formData = reactive<Record<string, any>>({ enableFlag: '' });

  const onSearch = (data: Record<string, any>) => {
    emit('search', cleanSearchParams(data));
  };

  const onReset = () => {
    resetSearchForm(formData, { enableFlag: '' });
    emit('reset');
  };
</script>
