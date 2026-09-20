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
    hide-sort
    :cursor-mode="cursorMode"
    :cursor-previous="cursorPrevious"
    :cursor-next="cursorNext"
    @refresh="$emit('refresh')"
    @reset="onReset"
    @search="onSearch"
    @size-change="$emit('size-change', $event)"
    @current-change="$emit('current-change', $event)"
    @cursor-previous="$emit('cursor-previous')"
    @cursor-next="$emit('cursor-next')"
  >
    <template #filters>
      <el-form-item v-if="embedded === ''" :label="$t('pointValue.tool.device')" prop="deviceId">
        <div class="tool-dictionary-field">
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
          <el-alert
            v-if="deviceError"
            :closable="false"
            :title="$t('common.optionLoadFailed')"
            class="tool-dictionary-error"
            show-icon
            type="error"
          >
            <el-button :loading="deviceLoading" link type="danger" @click="deviceDictionary('')">
              {{ $t('common.retry') }}
            </el-button>
          </el-alert>
        </div>
      </el-form-item>
      <el-form-item v-if="embedded === ''" :label="$t('pointValue.tool.point')" prop="pointId">
        <div class="tool-dictionary-field">
          <el-select
            v-model="formData.pointId"
            :disabled="!formData.deviceId"
            :loading="pointLoading"
            :placeholder="formData.deviceId ? $t('pointValue.tool.pointPlaceholder') : $t('pointValue.tool.pointDisabledPlaceholder')"
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
          <el-alert
            v-if="pointError"
            :closable="false"
            :title="$t('common.optionLoadFailed')"
            class="tool-dictionary-error"
            show-icon
            type="error"
          >
            <el-button :loading="pointLoading" link type="danger" @click="pointDictionary('')">
              {{ $t('common.retry') }}
            </el-button>
          </el-alert>
        </div>
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
        <enable-flag-segmented v-model="formData.enableFlag" include-all/>
      </el-form-item>
      <el-form-item :label="$t('settings.event.timeRange')" prop="rangeKey">
        <range-segmented v-model="formData.rangeKey" include-all/>
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
import {onBeforeUnmount, reactive, ref, watch} from 'vue';
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
  cursorMode: {type: Boolean, default: false},
  cursorPrevious: {type: Boolean, default: false},
  cursorNext: {type: Boolean, default: false},
});

const emit = defineEmits(['search', 'reset', 'refresh', 'size-change', 'current-change', 'cursor-previous', 'cursor-next']);

const formData = reactive<Record<string, any>>({enableFlag: '', rangeKey: ''});
const deviceDictionaries = ref<Dictionary[]>([]);
const deviceLoading = ref(false);
const deviceError = ref(false);
const pointDictionaries = ref<Dictionary[]>([]);
const pointLoading = ref(false);
const pointError = ref(false);
let deviceRequestId = 0;
let pointRequestId = 0;

const onSearch = (data: Record<string, any>) => {
  emit('search', cleanSearchParams(data));
};

const onReset = () => {
  deviceRequestId += 1;
  pointRequestId += 1;
  deviceLoading.value = false;
  pointLoading.value = false;
  deviceError.value = false;
  pointError.value = false;
  deviceDictionaries.value = [];
  pointDictionaries.value = [];
  resetSearchForm(formData, {enableFlag: '', rangeKey: ''});
  emit('reset');
};

const deviceDictionary = (query?: string) => {
  const requestId = ++deviceRequestId;
  deviceLoading.value = true;
  deviceError.value = false;
  listDeviceDictionary({
    offset: 0, limit: 50,
    label: query || '',
  })
    .then((res) => {
      if (requestId !== deviceRequestId) return;
      deviceDictionaries.value = res.items;
    })
    .catch(() => {
      if (requestId !== deviceRequestId) return;
      deviceDictionaries.value = [];
      deviceError.value = true;
    })
    .finally(() => {
      if (requestId === deviceRequestId) deviceLoading.value = false;
    });
};

const pointDictionary = (query?: string) => {
  // The backend dictionary contract requires a parent device
  // (requireParent → "Parent ID is required"); asking for points without
  // one is a guaranteed 4xx. The select is disabled until a device is
  // picked — this guard keeps the promise even for programmatic triggers.
  if (!formData.deviceId) {
    pointDictionaries.value = [];
    pointLoading.value = false;
    pointError.value = false;
    return;
  }
  const requestId = ++pointRequestId;
  const parentId = formData.deviceId || undefined;
  pointLoading.value = true;
  pointError.value = false;
  listPointDictionary({
    offset: 0, limit: 50,
    label: query || '',
    parentId,
  })
    .then((res) => {
      if (requestId !== pointRequestId || (formData.deviceId || undefined) !== parentId) return;
      pointDictionaries.value = res.items;
    })
    .catch(() => {
      if (requestId !== pointRequestId || (formData.deviceId || undefined) !== parentId) return;
      pointDictionaries.value = [];
      pointError.value = true;
    })
    .finally(() => {
      if (requestId === pointRequestId) pointLoading.value = false;
    });
};

const deviceDictionaryVisible = (visible: boolean) => {
  if (visible) deviceDictionary('');
};

const pointDictionaryVisible = (visible: boolean) => {
  if (visible) pointDictionary('');
};

watch(
  () => formData.deviceId,
  () => {
    formData.pointId = '';
    pointRequestId += 1;
    pointDictionaries.value = [];
    pointLoading.value = false;
    pointError.value = false;
  },
);

onBeforeUnmount(() => {
  deviceRequestId += 1;
  pointRequestId += 1;
});
</script>

<style lang="scss" scoped>
.tool-dictionary-field {
  display: grid;
  gap: var(--dc3-space-2);
  width: 100%;
  min-width: 0;
}

.tool-dictionary-error {
  margin: 0;

  :deep(.el-alert__content) {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--dc3-space-2);
  }
}
</style>
