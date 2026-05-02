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
      <el-form-item :label="$t('settings.user.nickName')" prop="nickName">
        <el-input
          v-model="formData.nickName"
          class="edit-form-default"
          clearable
          :placeholder="$t('settings.user.nickNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.user.userName')" prop="userName">
        <el-input
          v-model="formData.userName"
          class="edit-form-default"
          clearable
          :placeholder="$t('settings.user.userNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.user.phone')" prop="phone">
        <el-input
          v-model="formData.phone"
          class="edit-form-default"
          clearable
          :placeholder="$t('settings.user.phonePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('settings.user.email')" prop="email">
        <el-input
          v-model="formData.email"
          class="edit-form-default"
          clearable
          :placeholder="$t('settings.user.emailPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <el-segmented
          v-model="formData.enableFlag"
          :options="[
            { label: $t('common.all'), value: '' },
            { label: $t('common.enable'), value: 0 },
            { label: $t('common.disable'), value: 1 },
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

  const formData = reactive<Record<string, any>>({ enableFlag: '' });

  const onSearch = (data: Record<string, any>) => {
    const params = { ...data };
    // segmented "全部" 传空字符串时后端 Byte 反序列化会失败
    if (params.enableFlag === '' || params.enableFlag === undefined || params.enableFlag === null) {
      delete params.enableFlag;
    }
    emit('search', params);
  };

  const onReset = () => {
    Object.keys(formData).forEach((k) => delete formData[k]);
    formData.enableFlag = '';
    emit('reset');
  };
</script>
