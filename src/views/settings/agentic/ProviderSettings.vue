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
        <el-table-column prop="name" label="Name" min-width="160" show-overflow-tooltip />
        <el-table-column prop="providerType" label="Type" min-width="150" show-overflow-tooltip />
        <el-table-column prop="baseUrl" label="Base URL" min-width="220" show-overflow-tooltip />
        <el-table-column label="Default" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.defaultFlag) === 1 ? 'success' : 'info'" size="small">
              {{ Number(row.defaultFlag) === 1 ? $t('common.yes') : $t('common.no') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.enable')" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.enableFlag) === 0 ? 'success' : 'info'" size="small">
              {{ Number(row.enableFlag) === 0 ? $t('common.enable') : $t('common.disable') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="$t('common.remark')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="$t('common.operation')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.id" @click="openEdit(row)">{{
              $t('common.edit')
            }}</el-button>
            <el-popconfirm
              :title="`Delete provider ${row.name}?`"
              :confirm-button-text="$t('common.confirm')"
              :cancel-button-text="$t('common.cancel')"
              @confirm="remove(row)"
            >
              <template #reference>
                <el-button link type="danger" :disabled="!row.id">{{ $t('common.delete') }}</el-button>
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
  import { reactive, ref } from 'vue';

  import { addAgenticProvider, deleteAgenticProvider, getAgenticProviders, updateAgenticProvider } from '@/api/agentic';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import type { AgenticProvider } from '@/config/types';
  import { successMessage } from '@/utils/notificationUtil';

  import providerTool from './tool/ProviderTool.vue';
  import providerEditForm from './edit/ProviderEditForm.vue';

  const editRef = ref<InstanceType<typeof providerEditForm>>();

  const reactiveData = reactive({
    loading: false,
    listData: [] as AgenticProvider[],
    allData: [] as AgenticProvider[],
    query: {} as Record<string, any>,
    order: false,
    page: {
      total: 0,
      size: 12,
      current: 1,
      orders: [] as { column: string; asc: boolean }[],
    },
  });

  const load = async () => {
    reactiveData.loading = true;
    try {
      const response = await getAgenticProviders();
      reactiveData.allData = response.data || [];
      applyFilters();
    } finally {
      reactiveData.loading = false;
    }
  };

  const applyFilters = () => {
    let filtered = [...reactiveData.allData];
    const q = reactiveData.query;
    if (q.name) {
      const n = String(q.name).toLowerCase();
      filtered = filtered.filter((r) => r.name.toLowerCase().includes(n));
    }
    if (q.providerType) {
      const t = String(q.providerType).toLowerCase();
      filtered = filtered.filter((r) => r.providerType.toLowerCase().includes(t));
    }
    if (q.enableFlag !== '' && q.enableFlag !== undefined) {
      filtered = filtered.filter((r) => Number(r.enableFlag) === Number(q.enableFlag));
    }
    reactiveData.page.total = filtered.length;
    const start = (reactiveData.page.current - 1) * reactiveData.page.size;
    reactiveData.listData = filtered.slice(start, start + reactiveData.page.size);
  };

  const search = (params: Record<string, any>) => {
    reactiveData.query = params || {};
    reactiveData.page.current = 1;
    applyFilters();
  };

  const reset = () => {
    reactiveData.query = {};
    reactiveData.page.current = 1;
    applyFilters();
  };

  const refresh = () => load();

  const sort = () => {
    reactiveData.order = !reactiveData.order;
    reactiveData.page.orders = [{ column: 'create_time', asc: reactiveData.order }];
    reactiveData.allData.sort((a: Record<string, any>, b: Record<string, any>) => {
      const aTime = a.createTime || '';
      const bTime = b.createTime || '';
      const cmp = aTime.localeCompare(bTime);
      return reactiveData.order ? cmp : -cmp;
    });
    applyFilters();
  };

  const openAdd = () => editRef.value?.show();
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

  const sizeChange = (size: number) => {
    reactiveData.page.size = size;
    reactiveData.page.current = 1;
    applyFilters();
  };

  const currentChange = (current: number) => {
    reactiveData.page.current = current;
    applyFilters();
  };

  load();
</script>

<style lang="scss" scoped>
  .settings-table {
    margin-top: 1px;
    border-radius: 4px;
  }
</style>
