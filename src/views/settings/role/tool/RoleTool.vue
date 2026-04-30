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
    @search="onSearch"
    @reset="onReset"
    @refresh="$emit('refresh')"
    @sort="$emit('sort')"
    @size-change="$emit('size-change', $event)"
    @current-change="$emit('current-change', $event)"
  >
    <template #filters>
      <el-form-item :label="$t('settings.role.roleName')" prop="roleName">
        <el-input
          v-model="formData.roleName"
          class="edit-form-default"
          clearable
          :placeholder="$t('settings.role.roleNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.role.roleCode')" prop="roleCode">
        <el-input
          v-model="formData.roleCode"
          class="edit-form-default"
          clearable
          :placeholder="$t('settings.role.roleCodePlaceholder')"
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

  defineProps({
    page: {
      type: Object,
      required: true,
    },
  });

  const emit = defineEmits(['search', 'reset', 'refresh', 'sort', 'add', 'size-change', 'current-change']);

  const formData = reactive<Record<string, any>>({});

  const onSearch = (data: Record<string, any>) => {
    const params = { ...data };
    if (!params.enableFlag) delete params.enableFlag;
    emit('search', params);
  };

  const onReset = () => {
    Object.keys(formData).forEach((k) => delete formData[k]);
    emit('reset');
  };
</script>
