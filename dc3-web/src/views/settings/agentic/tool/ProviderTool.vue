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
        <el-input v-model="formData.name" :placeholder="$t('settings.agentic.providerNamePlaceholder')" clearable/>
      </el-form-item>
      <el-form-item :label="$t('settings.agentic.providerType')" prop="providerType">
        <el-select
          v-model="formData.providerType"
          :placeholder="$t('settings.agentic.providerTypeAllPlaceholder')"
          clearable
          style="width: 100%"
        >
          <el-option v-for="pt in providerTypes" :key="pt.value" :label="pt.label" :value="pt.value"/>
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="formData.enableFlag" include-all/>
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
