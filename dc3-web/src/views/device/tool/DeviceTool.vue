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
      <el-form-item :label="$t('device.tool.deviceName')" prop="deviceName">
        <el-input
          v-model="formData.deviceName"
          :placeholder="$t('device.tool.deviceNamePlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item v-if="embedded !== 'driver'" :label="$t('device.tool.driver')" prop="driverId">
        <el-select
          v-model="formData.driverId"
          :loading="driverLoading"
          :placeholder="$t('device.tool.driverPlaceholder')"
          :remote-method="driverDictionary"
          class="edit-form-special"
          clearable
          filterable
          remote
          reserve-keyword
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
        <enable-flag-segmented v-model="formData.enableFlag" include-all/>
      </el-form-item>
    </template>
    <template #actions>
      <el-button v-if="embedded === ''" :icon="Plus" type="success" @click="$emit('open-add')">
        {{ $t('common.add') }}
      </el-button>
      <el-button v-if="embedded === ''" :icon="Upload" type="primary" @click="$emit('open-import')">
        {{ $t('common.import') }}
      </el-button>
    </template>
  </tool-card>
</template>

<script lang="ts" setup>
import {reactive, ref} from 'vue';
import {Plus, Upload} from '@element-plus/icons-vue';
import ToolCard from '@/components/card/tool/ToolCard.vue';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import type {Dictionary} from '@/config/types';
import {listDriverDictionary} from '@/api/dictionary';
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

const emit = defineEmits([
  'search',
  'reset',
  'open-add',
  'open-import',
  'refresh',
  'sort',
  'size-change',
  'current-change',
]);

const formData = reactive<Record<string, any>>({enableFlag: ''});
const driverDictionaries = ref<Dictionary[]>([]);
const driverLoading = ref(false);

const onSearch = (data: Record<string, any>) => {
  emit('search', cleanSearchParams(data));
};

const onReset = () => {
  resetSearchForm(formData, {enableFlag: ''});
  emit('reset');
};

const driverDictionary = (query?: string) => {
  driverLoading.value = true;
  listDriverDictionary({
    page: {size: 50, current: 1},
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
