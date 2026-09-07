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
      <el-form-item :label="$t('point.tool.pointName')" prop="pointName">
        <el-input
          v-model="formData.pointName"
          :placeholder="$t('point.tool.pointNamePlaceholder')"
          class="edit-form-default"
          clearable
        />
      </el-form-item>
      <el-form-item
        v-if="embedded !== 'profile' && embedded !== 'edit'"
        :label="$t('point.tool.profile')"
        prop="profileId"
      >
        <div class="tool-dictionary-field">
          <el-select
            v-model="formData.profileId"
            :loading="profileLoading"
            :placeholder="$t('point.tool.profilePlaceholder')"
            :remote-method="profileDictionary"
            class="edit-form-special"
            clearable
            filterable
            remote
            reserve-keyword
            @visible-change="profileDictionaryVisible"
          >
            <el-option
              v-for="dictionary in profileDictionaries"
              :key="dictionary.value"
              :label="dictionary.label"
              :value="dictionary.value"
            />
          </el-select>
          <el-alert
            v-if="profileError"
            :closable="false"
            :title="$t('common.optionLoadFailed')"
            class="tool-dictionary-error"
            show-icon
            type="error"
          >
            <el-button :loading="profileLoading" link type="danger" @click="profileDictionary('')">
              {{ $t('common.retry') }}
            </el-button>
          </el-alert>
        </div>
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="formData.enableFlag" include-all/>
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
      <el-button v-if="embedded === '' || embedded === 'edit'" :icon="Plus" type="success" @click="$emit('open-add')">
        {{ $t('common.add') }}
      </el-button>
    </template>
  </tool-card>
</template>

<script lang="ts" setup>
import {onBeforeUnmount, reactive, ref} from 'vue';
import {Back, Check, Plus, RefreshLeft, Search} from '@element-plus/icons-vue';
import ToolCard from '@/components/card/tool/ToolCard.vue';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import type {Dictionary} from '@/config/types';
import {listProfileDictionary} from '@/api/dictionary';
import {cleanSearchParams, resetSearchForm} from '@/utils/searchParamUtil';

const props = defineProps({
  embedded: {
    type: String,
    default: '',
  },
  page: {
    type: Object,
    required: true,
  },
  pre: {
    type: Boolean,
    default: false,
  },
  next: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits([
  'search',
  'reset',
  'open-add',
  'refresh',
  'sort',
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
  profileRequestId += 1;
  profileLoading.value = false;
  profileError.value = false;
  profileDictionaries.value = [];
  resetSearchForm(formData, {enableFlag: ''});
  emit('reset');
};

const profileDictionaries = ref<Dictionary[]>([]);
const profileLoading = ref(false);
const profileError = ref(false);
let profileRequestId = 0;

const profileDictionary = (query?: string) => {
  const requestId = ++profileRequestId;
  profileLoading.value = true;
  profileError.value = false;
  listProfileDictionary({
    offset: 0, limit: 50,
    label: query || '',
  })
    .then((res) => {
      if (requestId !== profileRequestId) return;
      profileDictionaries.value = res.items;
    })
    .catch(() => {
      if (requestId !== profileRequestId) return;
      profileDictionaries.value = [];
      profileError.value = true;
    })
    .finally(() => {
      if (requestId === profileRequestId) profileLoading.value = false;
    });
};

const profileDictionaryVisible = (visible: boolean) => {
  if (visible) profileDictionary('');
};

if (props.embedded !== 'profile' && props.embedded !== 'edit') profileDictionary();

onBeforeUnmount(() => {
  profileRequestId += 1;
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
