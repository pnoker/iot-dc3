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
      </el-form-item>
      <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
        <enable-flag-segmented v-model="formData.enableFlag" include-all />
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
  import {reactive, ref} from 'vue';
  import {Back, Check, Plus, RefreshLeft, Search} from '@element-plus/icons-vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
  import type {Dictionary} from '@/config/types';
  import {listProfileDictionary} from '@/api/dictionary';
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
    resetSearchForm(formData, {enableFlag: ''});
    emit('reset');
  };

  const profileDictionaries = ref<Dictionary[]>([]);
  const profileLoading = ref(false);

  const profileDictionary = (query?: string) => {
    profileLoading.value = true;
    listProfileDictionary({
      page: {size: 50, current: 1},
      label: query || '',
    })
      .then((res) => {
        profileDictionaries.value = res.data.records;
      })
      .catch(() => {
        // nothing to do
      })
      .finally(() => {
        profileLoading.value = false;
      });
  };

  const profileDictionaryVisible = (visible: boolean) => {
    if (visible) profileDictionary('');
  };

  profileDictionary();
</script>
