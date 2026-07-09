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
      <el-form-item :label="$t('settings.serviceAccount.serviceAccountName')" prop="serviceAccountName">
        <el-input
          v-model="formData.serviceAccountName"
          :placeholder="$t('settings.serviceAccount.serviceAccountNamePlaceholder')"
          class="edit-form-default"
          clearable
        />
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

defineProps({
  page: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(['search', 'reset', 'refresh', 'sort', 'add', 'size-change', 'current-change']);

const formData = reactive<Record<string, any>>({serviceAccountName: '', enableFlag: ''});

const onSearch = (data: Record<string, any>) => {
  emit('search', cleanSearchParams(data));
};

const onReset = () => {
  resetSearchForm(formData, {serviceAccountName: '', enableFlag: ''});
  emit('reset');
};
</script>
