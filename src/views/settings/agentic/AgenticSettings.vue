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
    <model-config-tool
      :page="reactiveData.page"
      :providers="reactiveData.providers"
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
        <el-table-column prop="label" label="Label" min-width="160" show-overflow-tooltip />
        <el-table-column prop="model" label="Model" min-width="180" show-overflow-tooltip />
        <el-table-column prop="providerName" label="Provider" min-width="150" show-overflow-tooltip />
        <el-table-column label="Capabilities" min-width="240">
          <template #default="{ row }">
            <div class="agentic-tags">
              <el-tag :type="row.stream ? 'success' : 'info'" size="small">Stream</el-tag>
              <el-tag :type="row.toolCall ? 'success' : 'info'" size="small">Tools</el-tag>
              <el-tag :type="row.vision ? 'success' : 'info'" size="small">Vision</el-tag>
              <el-tag :type="row.reasoning ? 'success' : 'info'" size="small">Reasoning</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Default" width="100">
          <template #default="{ row }">
            <el-tag :type="row.defaultFlag === 'DEFAULT' ? 'success' : 'info'" size="small">
              {{ row.defaultFlag === 'DEFAULT' ? $t('common.yes') : $t('common.no') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.enable')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enableFlag === 'ENABLE' ? 'success' : 'info'" size="small">
              {{ row.enableFlag === 'ENABLE' ? $t('common.enable') : $t('common.disable') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="temperature" label="Temp" width="80" />
        <el-table-column prop="maxTokens" label="Tokens" width="90" />
        <el-table-column prop="remark" :label="$t('common.remark')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="$t('common.operation')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.id" @click="openEdit(row)">{{
              $t('common.edit')
            }}</el-button>
            <el-popconfirm
              :title="`Delete model ${row.model}?`"
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

    <model-config-edit-form ref="editRef" :providers="reactiveData.providers" @save="onSave" />
  </div>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';

  import {
    addAgenticModelConfig,
    deleteAgenticModelConfig,
    getAgenticModelConfigs,
    getAgenticProviders,
    updateAgenticModelConfig,
  } from '@/api/agentic';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import type { AgenticModelConfig, AgenticProvider } from '@/config/types';
  import { successMessage } from '@/utils/notificationUtil';

  import modelConfigTool from './tool/ModelConfigTool.vue';
  import modelConfigEditForm from './edit/ModelConfigEditForm.vue';

  const editRef = ref<InstanceType<typeof modelConfigEditForm>>();

  const reactiveData = reactive({
    loading: false,
    listData: [] as AgenticModelConfig[],
    allData: [] as AgenticModelConfig[],
    providers: [] as AgenticProvider[],
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
      const [configResponse, providerResponse] = await Promise.all([getAgenticModelConfigs(), getAgenticProviders()]);
      reactiveData.allData = configResponse.data || [];
      reactiveData.providers = providerResponse.data || [];
      applyFilters();
    } finally {
      reactiveData.loading = false;
    }
  };

  const applyFilters = () => {
    let filtered = [...reactiveData.allData];
    const q = reactiveData.query;
    if (q.model) {
      const m = String(q.model).toLowerCase();
      filtered = filtered.filter((r) => r.model.toLowerCase().includes(m));
    }
    if (q.providerId) {
      filtered = filtered.filter((r) => String(r.providerId) === String(q.providerId));
    }
    if (q.enableFlag) {
      filtered = filtered.filter((r) => r.enableFlag === q.enableFlag);
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
    reactiveData.allData.sort((a, b) => {
      const cmp = (a.createTime || '').localeCompare(b.createTime || '');
      return reactiveData.order ? cmp : -cmp;
    });
    applyFilters();
  };

  const openAdd = () => editRef.value?.show();
  const openEdit = (row: AgenticModelConfig) => editRef.value?.showEdit(row);

  const onSave = (form: AgenticModelConfig, done: () => void) => {
    const apiCall = form.id ? updateAgenticModelConfig(form) : addAgenticModelConfig(form);
    apiCall
      .then(() => {
        successMessage();
        load();
        done();
      })
      .catch(() => {});
  };

  const remove = (row: AgenticModelConfig) => {
    if (!row.id) return;
    deleteAgenticModelConfig(String(row.id))
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

  .agentic-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
</style>
