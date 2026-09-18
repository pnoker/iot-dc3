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
    <provider-tool
      :page="reactiveData.page"
      @add="openAdd"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @sort="sort"
      @size-change="sizeChange"
      @current-change="currentChange"
    />

    <!-- @vue-generic {AgenticProvider} -->
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
          :title="t('settings.agentic.confirmDeleteProvider', {name: row.name})"
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

    <provider-edit-form ref="editRef" @save="onSave"/>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';

import {addAgenticProvider, deleteAgenticProvider, listAgenticProviders, updateAgenticProvider} from '@/api/agentic';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import DefaultTag from '@/components/tag/DefaultTag.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import {usePagedList} from '@/composables/usePagedList';
import type {AgenticProvider, ResponsiveListColumn} from '@/config/types';
import {successMessage} from '@/utils/notificationUtil';

import providerTool from './tool/ProviderTool.vue';
import providerEditForm from './edit/ProviderEditForm.vue';

const router = useRouter();
const {t} = useI18n();
const editRef = ref<InstanceType<typeof providerEditForm>>();
const deletingIds = ref(new Set<string>());
let disposed = false;

const columns = computed<ResponsiveListColumn<AgenticProvider>[]>(() => [
  {key: 'name', label: t('settings.agentic.providerName'), minWidth: 160, mobile: 'primary'},
  {key: 'providerType', label: t('settings.agentic.providerType'), minWidth: 150, mobile: 'detail'},
  {key: 'baseUrl', label: t('settings.agentic.baseUrl'), minWidth: 200, mobile: 'detail'},
  {key: 'defaultFlag', label: t('settings.agentic.default'), width: 100, kind: 'default', mobile: 'detail'},
  {key: 'enableFlag', label: t('common.enable'), width: 100, kind: 'enable', mobile: 'detail'},
  {key: 'remark', label: t('common.remark'), minWidth: 140, mobile: 'hidden'},
]);

interface ProviderQuery {
  name?: string;
  providerType?: string;
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
} = usePagedList<AgenticProvider, ProviderQuery>({
  filter: (rows, query) => {
    let filtered = rows;
    if (query.name) {
      const name = String(query.name).toLowerCase();
      filtered = filtered.filter((row) => String(row.name ?? '').toLowerCase().includes(name));
    }
    if (query.providerType) {
      const providerType = String(query.providerType).toLowerCase();
      filtered = filtered.filter((row) => String(row.providerType ?? '').toLowerCase().includes(providerType));
    }
    if (query.enableFlag) {
      filtered = filtered.filter((row) => row.enableFlag === query.enableFlag);
    }
    return filtered;
  },
  sortValue: (row) => row.createTime,
});

const load = () =>
  loadClientData(listAgenticProviders, (response) => {
    if (disposed) return;
    setAllData(response || []);
  });

const refresh = () => load();

const openAdd = () => editRef.value?.show();
const openDetail = (row: AgenticProvider) => {
  router.push({name: 'settingsModelProviderDetail', query: {id: String(row.id)}}).catch(() => {
    // handled globally
  });
};
const openEdit = (row: AgenticProvider) => editRef.value?.showEdit(row);

const onSave = (form: AgenticProvider, done: (close?: boolean) => void) => {
  const apiCall = form.id ? updateAgenticProvider(form) : addAgenticProvider(form);
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

const remove = (row: AgenticProvider) => {
  const id = String(row.id || '');
  if (!id || deletingIds.value.has(id)) return;
  deletingIds.value.add(id);
  deleteAgenticProvider(id)
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

const isDeleting = (row: AgenticProvider) => deletingIds.value.has(String(row.id));

onBeforeUnmount(() => {
  disposed = true;
  deletingIds.value.clear();
});

void load();
</script>
