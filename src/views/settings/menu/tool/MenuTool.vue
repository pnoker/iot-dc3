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
  <tool-card :form-model="formData" :page="page" @search="onSearch" @reset="onReset" @refresh="$emit('refresh')">
    <template #filters>
      <el-form-item :label="$t('settings.menu.menuName')" prop="menuName">
        <el-input
          v-model="formData.menuName"
          class="edit-form-default"
          clearable
          :placeholder="$t('settings.menu.menuNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.menu.menuCode')" prop="menuCode">
        <el-input
          v-model="formData.menuCode"
          class="edit-form-default"
          clearable
          :placeholder="$t('settings.menu.menuCodePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.menu.menuType')" prop="menuTypeFlag">
        <el-select v-model="formData.menuTypeFlag" clearable class="edit-form-default" :placeholder="$t('common.all')">
          <el-option v-for="opt in MENU_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
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
  import { MENU_TYPE_OPTIONS } from '@/config/constant/enums';

  defineProps({
    page: {
      type: Object,
      required: true,
    },
  });

  const emit = defineEmits(['search', 'reset', 'refresh', 'add']);

  const formData = reactive<Record<string, any>>({ enableFlag: '' });

  const onSearch = (data: Record<string, any>) => {
    const params = { ...data };
    if (!params.enableFlag) delete params.enableFlag;
    if (!params.menuTypeFlag) delete params.menuTypeFlag;
    emit('search', params);
  };

  const onReset = () => {
    Object.keys(formData).forEach((k) => delete formData[k]);
    formData.enableFlag = '';
    emit('reset');
  };
</script>
