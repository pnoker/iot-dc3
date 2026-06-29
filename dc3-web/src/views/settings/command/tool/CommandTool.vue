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
      <el-form-item :label="$t('command.tool.commandName')" prop="commandName">
        <el-input
          v-model="formData.commandName"
          :placeholder="$t('command.tool.commandNamePlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('command.card.code')" prop="commandCode">
        <el-input
          v-model="formData.commandCode"
          :placeholder="$t('command.card.code')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="formData.enableFlag" include-all />
      </el-form-item>
    </template>
    <template v-if="pre || next" #buttons="{search, reset}">
      <el-button v-if="pre" :icon="Back" plain @click="$emit('pre-handle')">
        {{ $t('common.previous') }}
      </el-button>
      <el-button :icon="Search" type="primary" @click="search">{{ $t('common.search') }}</el-button>
      <el-button :icon="RefreshLeft" @click="reset">{{ $t('common.reset') }}</el-button>
      <el-button v-if="next" :icon="Check" plain type="primary" @click="$emit('next-handle')">
        {{ $t('common.next') }}
      </el-button>
    </template>
    <template #actions>
      <el-button v-if="editable" :icon="Plus" type="success" @click="$emit('add')">
        {{ $t('common.add') }}
      </el-button>
    </template>
  </tool-card>
</template>

<script lang="ts" setup>
  import {reactive} from 'vue';
  import {Back, Check, Plus, RefreshLeft, Search} from '@element-plus/icons-vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
  import {cleanSearchParams, resetSearchForm} from '@/utils/searchParamUtil';

  defineProps({
    page: {type: Object, required: true},
    editable: {type: Boolean, default: true},
    pre: {type: Boolean, default: false},
    next: {type: Boolean, default: false},
  });

  const emit = defineEmits([
    'search',
    'reset',
    'refresh',
    'sort',
    'add',
    'size-change',
    'current-change',
    'pre-handle',
    'next-handle',
  ]);

  const formData = reactive<Record<string, any>>({enableFlag: ''});

  const onSearch = (data: Record<string, any>) => {
    emit('search', cleanSearchParams(data));
  };

  const onReset = () => {
    resetSearchForm(formData, {enableFlag: ''});
    emit('reset');
  };
</script>
