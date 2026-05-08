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
    @search="$emit('search', $event)"
    @reset="$emit('reset')"
    @refresh="$emit('refresh')"
    @sort="$emit('sort')"
    @size-change="$emit('size-change', $event)"
    @current-change="$emit('current-change', $event)"
  >
    <template #filters>
      <el-form-item :label="$t('device.tool.deviceName')" prop="deviceName">
        <el-input
          v-model="formData.deviceName"
          class="edit-form-default"
          clearable
          :placeholder="$t('device.tool.deviceNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item v-if="embedded !== 'driver'" :label="$t('device.tool.driver')" prop="driverId">
        <el-select
          v-model="formData.driverId"
          class="edit-form-special"
          clearable
          filterable
          remote
          reserve-keyword
          :placeholder="$t('device.tool.driverPlaceholder')"
          :remote-method="driverDictionary"
          :loading="driverLoading"
          @visible-change="driverDictionaryVisible"
        >
          <el-option
            v-for="dictionary in driverDictionaries"
            :key="dictionary.value"
            :label="dictionary.label"
            :value="dictionary.value"
          />
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
      <el-button v-if="embedded === ''" :icon="Plus" type="success" @click="$emit('show-add')">
        {{ $t('common.add') }}
      </el-button>
      <el-button v-if="embedded === ''" :icon="Upload" type="primary" @click="$emit('show-import')">
        {{ $t('common.import') }}
      </el-button>
    </template>
  </tool-card>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { Plus, Upload } from '@element-plus/icons-vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import type { Dictionary } from '@/config/types';
  import { getDriverDictionary } from '@/api/dictionary';

  defineProps({
    embedded: {
      type: String,
      default: '',
    },
    page: {
      type: Object,
      required: true,
    },
  });

  defineEmits(['search', 'reset', 'show-add', 'show-import', 'refresh', 'sort', 'size-change', 'current-change']);

  const formData = reactive<Record<string, any>>({ enableFlag: '' });
  const driverDictionaries = ref<Dictionary[]>([]);
  const driverLoading = ref(false);

  const driverDictionary = (query?: string) => {
    driverLoading.value = true;
    getDriverDictionary({
      page: { size: 50, current: 1 },
      label: query || '',
    })
      .then((res) => {
        driverDictionaries.value = res.data.records;
      })
      .catch(() => {
        // nothing to do
      })
      .finally(() => {
        driverLoading.value = false;
      });
  };

  const driverDictionaryVisible = (visible: boolean) => {
    if (visible) driverDictionary('');
  };
</script>
