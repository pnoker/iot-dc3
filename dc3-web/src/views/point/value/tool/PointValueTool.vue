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
    hide-sort
    @refresh="$emit('refresh')"
    @reset="onReset"
    @search="onSearch"
    @size-change="$emit('size-change', $event)"
    @current-change="$emit('current-change', $event)"
  >
    <template #filters>
      <el-form-item v-if="embedded === ''" :label="$t('pointValue.tool.device')" prop="deviceId">
        <el-select
          v-model="formData.deviceId"
          :loading="deviceLoading"
          :placeholder="$t('pointValue.tool.devicePlaceholder')"
          :remote-method="deviceDictionary"
          class="edit-form-special"
          clearable
          filterable
          remote
          reserve-keyword
          @visible-change="deviceDictionaryVisible"
        >
          <el-option
            v-for="dictionary in deviceDictionaries"
            :key="dictionary.value"
            :label="dictionary.label"
            :value="dictionary.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="embedded === ''" :label="$t('pointValue.tool.point')" prop="pointId">
        <el-select
          v-model="formData.pointId"
          :loading="pointLoading"
          :placeholder="$t('pointValue.tool.pointPlaceholder')"
          :remote-method="pointDictionary"
          class="edit-form-special"
          clearable
          filterable
          remote
          reserve-keyword
          @visible-change="pointDictionaryVisible"
        >
          <el-option
            v-for="dictionary in pointDictionaries"
            :key="dictionary.value"
            :label="dictionary.label"
            :value="dictionary.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="embedded === 'device'" :label="$t('pointValue.tool.pointName')" prop="pointName">
        <el-input
          v-model="formData.pointName"
          :placeholder="$t('pointValue.tool.pointNamePlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item v-if="embedded === 'device'" :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="formData.enableFlag" include-all />
      </el-form-item>
      <el-form-item :label="$t('settings.event.timeRange')" prop="rangeKey">
        <range-segmented v-model="formData.rangeKey" include-all />
      </el-form-item>
    </template>
    <template #actions>
      <el-button v-if="embedded === ''" :icon="Plus" disabled type="success">
        {{ $t('common.add') }}
      </el-button>
    </template>
  </tool-card>
</template>

<script lang="ts" setup>
  import {reactive, ref} from 'vue';
  import {Plus} from '@element-plus/icons-vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
  import RangeSegmented from '@/components/segmented/RangeSegmented.vue';
  import type {Dictionary} from '@/config/types';
  import {listDeviceDictionary, listPointDictionary} from '@/api/dictionary';
  import {cleanSearchParams, resetSearchForm} from '@/utils/searchParamUtil';

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

  const emit = defineEmits(['search', 'reset', 'refresh', 'size-change', 'current-change']);

  const formData = reactive<Record<string, any>>({enableFlag: '', rangeKey: ''});
  const deviceDictionaries = ref<Dictionary[]>([]);
  const deviceLoading = ref(false);
  const pointDictionaries = ref<Dictionary[]>([]);
  const pointLoading = ref(false);

  const onSearch = (data: Record<string, any>) => {
    emit('search', cleanSearchParams(data));
  };

  const onReset = () => {
    resetSearchForm(formData, {enableFlag: '', rangeKey: ''});
    emit('reset');
  };

  const deviceDictionary = (query?: string) => {
    deviceLoading.value = true;
    listDeviceDictionary({
      page: {size: 50, current: 1},
      label: query || '',
    })
      .then((res) => {
        deviceDictionaries.value = res.data.records;
      })
      .catch(() => {
        // nothing to do
      })
      .finally(() => {
        deviceLoading.value = false;
      });
  };

  const pointDictionary = (query?: string) => {
    pointLoading.value = true;
    listPointDictionary({
      page: {size: 50, current: 1},
      label: query || '',
      parentId: formData.deviceId,
    })
      .then((res) => {
        pointDictionaries.value = res.data.records;
      })
      .catch(() => {
        // nothing to do
      })
      .finally(() => {
        pointLoading.value = false;
      });
  };

  const deviceDictionaryVisible = (visible: boolean) => {
    if (visible) deviceDictionary('');
  };

  const pointDictionaryVisible = (visible: boolean) => {
    if (visible) pointDictionary('');
  };
</script>
