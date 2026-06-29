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

    <blank-card>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <el-table-column
          :label="$t('settings.agentic.providerName')"
          min-width="160"
          prop="name"
          show-overflow-tooltip
        />
        <el-table-column
          :label="$t('settings.agentic.providerType')"
          min-width="150"
          prop="providerType"
          show-overflow-tooltip
        />
        <el-table-column :label="$t('settings.agentic.baseUrl')" min-width="200" prop="baseUrl" show-overflow-tooltip />
        <el-table-column :label="$t('settings.agentic.default')" width="100">
          <template #default="{row}">
            <default-tag :value="row.defaultFlag" size="small" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.enable')" width="100">
          <template #default="{row}">
            <enable-tag :value="row.enableFlag" size="small" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.remark')" min-width="140" prop="remark" show-overflow-tooltip />
        <!-- @vue-generic {AgenticProvider} -->
        <el-table-column :label="$t('common.operation')" fixed="right" width="210">
          <template #default="{row}">
            <el-button :disabled="!row.id" link type="primary" @click="openDetail(row)"
              >{{ $t('common.detail') }}
            </el-button>
            <el-button :disabled="!row.id" link type="primary" @click="openEdit(row)"
              >{{ $t('common.edit') }}
            </el-button>
            <el-popconfirm
              :cancel-button-text="$t('common.cancel')"
              :confirm-button-text="$t('common.confirm')"
              :title="$t('settings.agentic.confirmDeleteProvider', {name: row.name})"
              @confirm="remove(row)"
            >
              <template #reference>
                <el-button :disabled="!row.id" link type="danger">{{ $t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="$t('common.empty')" />
        </template>
      </el-table>
    </blank-card>

    <provider-edit-form ref="editRef" @save="onSave" />
  </div>
</template>

<script lang="ts" setup>
  import {ref} from 'vue';
  import {useRouter} from 'vue-router';

  import {addAgenticProvider, deleteAgenticProvider, listAgenticProviders, updateAgenticProvider} from '@/api/agentic';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import DefaultTag from '@/components/tag/DefaultTag.vue';
  import EnableTag from '@/components/tag/EnableTag.vue';
  import {usePagedList} from '@/composables/usePagedList';
  import type {AgenticProvider} from '@/config/types';
  import {successMessage} from '@/utils/notificationUtil';

  import providerTool from './tool/ProviderTool.vue';
  import providerEditForm from './edit/ProviderEditForm.vue';

  const router = useRouter();
  const editRef = ref<InstanceType<typeof providerEditForm>>();

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
    withLoading,
  } = usePagedList<AgenticProvider, ProviderQuery>({
    filter: (rows, query) => {
      let filtered = rows;
      if (query.name) {
        const name = String(query.name).toLowerCase();
        filtered = filtered.filter((row) => row.name.toLowerCase().includes(name));
      }
      if (query.providerType) {
        const providerType = String(query.providerType).toLowerCase();
        filtered = filtered.filter((row) => row.providerType.toLowerCase().includes(providerType));
      }
      if (query.enableFlag) {
        filtered = filtered.filter((row) => row.enableFlag === query.enableFlag);
      }
      return filtered;
    },
    sortValue: (row) => row.createTime,
  });

  const load = () =>
    withLoading(async () => {
      const response = await listAgenticProviders();
      setAllData(response.data || []);
    });

  const refresh = () => load();

  const openAdd = () => editRef.value?.show();
  const openDetail = (row: AgenticProvider) => {
    router.push({name: 'settingsModelProviderDetail', query: {id: String(row.id)}}).catch(() => {
      // handled globally
    });
  };
  const openEdit = (row: AgenticProvider) => editRef.value?.showEdit(row);

  const onSave = (form: AgenticProvider, done: () => void) => {
    const apiCall = form.id ? updateAgenticProvider(form) : addAgenticProvider(form);
    apiCall
      .then(() => {
        successMessage();
        load();
        done();
      })
      .catch(() => {});
  };

  const remove = (row: AgenticProvider) => {
    if (!row.id) return;
    deleteAgenticProvider(String(row.id))
      .then(() => {
        successMessage();
        load();
      })
      .catch(() => {});
  };

  load();
</script>
