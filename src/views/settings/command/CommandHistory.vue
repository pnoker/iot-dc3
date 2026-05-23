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
    <tool-card
      :form-model="formData"
      :page="reactiveData.page"
      @refresh="refresh"
      @reset="onReset"
      @search="onSearch"
      @size-change="sizeChange"
      @current-change="currentChange"
    >
      <template #filters>
        <el-form-item label="Device ID" prop="deviceId">
          <el-input v-model="formData.deviceId" placeholder="Device ID" class="edit-form-default" clearable />
        </el-form-item>
        <el-form-item label="Command Code" prop="commandCode">
          <el-input v-model="formData.commandCode" placeholder="Command Code" class="edit-form-default" clearable />
        </el-form-item>
        <el-form-item label="Status" prop="status">
          <el-input v-model="formData.status" placeholder="Status" class="edit-form-default" clearable />
        </el-form-item>
      </template>
    </tool-card>

    <blank-card>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <el-table-column label="Record ID" min-width="180" prop="recordId" show-overflow-tooltip />
        <el-table-column label="Device ID" min-width="160" prop="deviceId" show-overflow-tooltip />
        <el-table-column label="Command Code" min-width="140" prop="commandCode" />
        <el-table-column label="Status" width="100" prop="status" />
        <el-table-column label="Error" min-width="180" prop="errorMessage" show-overflow-tooltip />
        <el-table-column :formatter="timestampColumn" label="Occur Time" prop="occurTime" width="165" />
        <el-table-column :formatter="timestampColumn" :label="$t('common.createTime')" prop="createTime" width="165" />
        <el-table-column :label="$t('common.operation')" fixed="right" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ $t('common.detail') }}</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="$t('common.description')" />
        </template>
      </el-table>
    </blank-card>

    <el-dialog v-model="detailVisible" :append-to-body="true" title="Record Detail" width="700px" draggable>
      <el-descriptions v-if="detailRow" :column="2" border>
        <el-descriptions-item label="Record ID" :span="2">{{ detailRow.recordId }}</el-descriptions-item>
        <el-descriptions-item label="Device ID">{{ detailRow.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="Command ID">{{ detailRow.commandId }}</el-descriptions-item>
        <el-descriptions-item label="Command Code">{{ detailRow.commandCode }}</el-descriptions-item>
        <el-descriptions-item label="Status">{{ detailRow.status }}</el-descriptions-item>
        <el-descriptions-item label="Error Code">{{ detailRow.errorCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Error Message">{{ detailRow.errorMessage || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Source">{{ detailRow.source || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Source User ID">{{ detailRow.sourceUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Param Values" :span="2">{{
          JSON.stringify(detailRow.paramValues) || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="Result Values" :span="2">{{
          JSON.stringify(detailRow.resultValues) || '-'
        }}</el-descriptions-item>
        <el-descriptions-item :formatter="timestampColumn" label="Occur Time">{{
          detailRow.occurTime || '-'
        }}</el-descriptions-item>
        <el-descriptions-item :formatter="timestampColumn" label="Send Time">{{
          detailRow.sendTime || '-'
        }}</el-descriptions-item>
        <el-descriptions-item :formatter="timestampColumn" label="Finish Time">{{
          detailRow.finishTime || '-'
        }}</el-descriptions-item>
        <el-descriptions-item :formatter="timestampColumn" label="Expire Time">{{
          detailRow.expireTime || '-'
        }}</el-descriptions-item>
        <el-descriptions-item :formatter="timestampColumn" label="Create Time">{{
          detailRow.createTime || '-'
        }}</el-descriptions-item>
        <el-descriptions-item :formatter="timestampColumn" label="Operate Time">{{
          detailRow.operateTime || '-'
        }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { getCommandHistoryById, listCommandHistory } from '@/api/command';
  import { timestampColumn } from '@/utils/dateUtil';
  import type { Order } from '@/config/types';
  import type { CommandHistory } from '@/config/types';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import { cleanSearchParams, resetSearchForm } from '@/utils/searchParamUtil';

  const reactiveData = reactive({
    loading: false,
    listData: [] as CommandHistory[],
    query: {} as Record<string, unknown>,
    page: { total: 0, size: 12, current: 1, orders: [] as Order[] },
  });

  const formData = reactive<Record<string, any>>({});
  const detailVisible = ref(false);
  const detailRow = ref<CommandHistory | null>(null);

  const load = () => {
    reactiveData.loading = true;
    listCommandHistory({ page: reactiveData.page, ...reactiveData.query })
      .then((res) => {
        const data = res.data || {};
        reactiveData.listData = data.records || [];
        reactiveData.page.total = data.total || 0;
      })
      .finally(() => {
        reactiveData.loading = false;
      });
  };

  const onSearch = (data: Record<string, any>) => {
    reactiveData.query = cleanSearchParams(data);
    reactiveData.page.current = 1;
    load();
  };

  const onReset = () => {
    resetSearchForm(formData, {});
    reactiveData.query = {};
    reactiveData.page.current = 1;
    load();
  };

  const refresh = () => load();

  const sizeChange = (size: number) => {
    reactiveData.page.size = size;
    load();
  };
  const currentChange = (current: number) => {
    reactiveData.page.current = current;
    load();
  };

  const openDetail = (row: CommandHistory) => {
    getCommandHistoryById(row.recordId)
      .then((res) => {
        detailRow.value = res.data || row;
        detailVisible.value = true;
      })
      .catch(() => {
        detailRow.value = row;
        detailVisible.value = true;
      });
  };

  load();
</script>

<style lang="scss" scoped>
  .settings-table {
    margin-top: 1px;
    border-radius: 4px;
  }
</style>
