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
  <div>
    <model-config-tool
      :page="reactiveData.page"
      :providers="providers"
      @add="openAdd"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @sort="sort"
      @size-change="sizeChange"
      @current-change="currentChange"
    />

    <!-- @vue-generic {AgenticModelConfig} -->
    <responsive-record-list
      :columns="columns"
      :loading="reactiveData.loading"
      :rows="reactiveData.listData"
      :status="reactiveData.status"
      openable
      operation-width="210"
      @open="openDetail"
      @retry="refresh"
    >
      <template #cell-capabilities="{row}">
        <div class="agentic-tags">
          <el-tag :type="row.stream ? 'success' : 'info'" size="small">{{ t('agentic.capStream') }}</el-tag>
          <el-tag :type="row.toolCall ? 'success' : 'info'" size="small">{{ t('agentic.capTools') }}</el-tag>
          <el-tag :type="row.vision ? 'success' : 'info'" size="small">{{ t('agentic.capVision') }}</el-tag>
          <el-tag :type="row.reasoning ? 'success' : 'info'" size="small">{{ t('agentic.capReasoning') }}</el-tag>
        </div>
      </template>
      <template #cell-defaultFlag="{row}">
        <default-tag :value="row.defaultFlag" size="small"/>
      </template>
      <template #cell-enableFlag="{row}">
        <enable-tag :value="row.enableFlag" size="small"/>
      </template>
      <template #actions="{row}">
        <el-button :disabled="!row.id || isDeleting(row)" link type="primary" @click="openDetail(row)">
          {{ t('common.detail') }}
        </el-button>
        <el-button :disabled="!row.id || isDeleting(row)" link type="primary" @click="openEdit(row)">
          {{ t('common.edit') }}
        </el-button>
        <el-popconfirm
          :cancel-button-text="t('common.cancel')"
          :confirm-button-text="t('common.confirm')"
          :title="t('settings.agentic.confirmDeleteModel', {name: row.model})"
          @confirm="remove(row)"
        >
          <template #reference>
            <el-button :disabled="!row.id" :loading="isDeleting(row)" link type="danger">
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-popconfirm>
      </template>
    </responsive-record-list>

    <model-config-edit-form ref="editRef" :providers="providers" @save="onSave"/>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';

import {
  addAgenticModelConfig,
  deleteAgenticModelConfig,
  listAgenticModelConfigs,
  listAgenticProviders,
  updateAgenticModelConfig,
} from '@/api/agentic';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import DefaultTag from '@/components/tag/DefaultTag.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import {usePagedList} from '@/composables/usePagedList';
import type {AgenticModelConfig, AgenticProvider, ResponsiveListColumn} from '@/config/types';
import {successMessage} from '@/utils/notificationUtil';

import modelConfigTool from './tool/ModelConfigTool.vue';
import modelConfigEditForm from './edit/ModelConfigEditForm.vue';

const router = useRouter();
const {t} = useI18n();
const editRef = ref<InstanceType<typeof modelConfigEditForm>>();
const providers = ref<AgenticProvider[]>([]);
const deletingIds = ref(new Set<string>());
let disposed = false;

const columns = computed<ResponsiveListColumn<AgenticModelConfig>[]>(() => [
  {key: 'label', label: t('settings.agentic.label'), minWidth: 160, mobile: 'primary'},
  {key: 'model', label: t('settings.agentic.model'), minWidth: 180, mobile: 'detail'},
  {key: 'providerName', label: t('settings.agentic.provider'), minWidth: 150, mobile: 'detail'},
  {
    key: 'capabilities',
    label: t('settings.agentic.capabilities'),
    minWidth: 210,
    kind: 'custom',
    mobile: 'detail',
  },
  {key: 'defaultFlag', label: t('settings.agentic.default'), width: 100, kind: 'default', mobile: 'detail'},
  {key: 'enableFlag', label: t('common.enable'), width: 100, kind: 'enable', mobile: 'detail'},
  {key: 'remark', label: t('common.remark'), minWidth: 140, mobile: 'hidden'},
]);

interface ModelConfigQuery {
  model?: string;
  providerId?: string;
  enableFlag?: string;
}

const {
  state: reactiveData,
  setAllData,
  search,
  reset,
  sort,
  sizeChange,
  currentChange,
  loadClientData,
} = usePagedList<AgenticModelConfig, ModelConfigQuery>({
  filter: (rows, query) => {
    let filtered = rows;
    if (query.model) {
      const model = String(query.model).toLowerCase();
      filtered = filtered.filter((row) => String(row.model ?? '').toLowerCase().includes(model));
    }
    if (query.providerId) {
      filtered = filtered.filter((row) => String(row.providerId) === String(query.providerId));
    }
    if (query.enableFlag) {
      filtered = filtered.filter((row) => row.enableFlag === query.enableFlag);
    }
    return filtered;
  },
  sortValue: (row) => row.createTime,
});

const load = () =>
  loadClientData(
    () => Promise.all([listAgenticModelConfigs(), listAgenticProviders()]),
    ([configResponse, providerResponse]) => {
      if (disposed) return;
      providers.value = providerResponse || [];
      setAllData(configResponse || []);
    }
  );

const refresh = () => load();

const openAdd = () => editRef.value?.show();
const openDetail = (row: AgenticModelConfig) => {
  router.push({name: 'settingsModelConfigDetail', query: {id: String(row.id)}}).catch(() => {
    // handled globally
  });
};
const openEdit = (row: AgenticModelConfig) => editRef.value?.showEdit(row);

const onSave = (form: AgenticModelConfig, done: (close?: boolean) => void) => {
  const apiCall = form.id ? updateAgenticModelConfig(form) : addAgenticModelConfig(form);
  apiCall
    .then(() => {
      if (disposed) return;
      successMessage();
      void load();
      done();
    })
    .catch(() => {
      if (!disposed) done(false);
    });
};

const remove = (row: AgenticModelConfig) => {
  const id = String(row.id || '');
  if (!id || deletingIds.value.has(id)) return;
  deletingIds.value.add(id);
  deleteAgenticModelConfig(String(row.id))
    .then(() => {
      if (disposed) return;
      successMessage();
      void load();
    })
    .catch(() => {
      // handled globally
    })
    .finally(() => {
      deletingIds.value.delete(id);
    });
};

const isDeleting = (row: AgenticModelConfig) => deletingIds.value.has(String(row.id));

onBeforeUnmount(() => {
  disposed = true;
  deletingIds.value.clear();
});

void load();
</script>

<style lang="scss" scoped>
.agentic-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--dc3-space-2);
}
</style>
