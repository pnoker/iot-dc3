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
        <el-form-item :label="$t('commandHistory.deviceId')" prop="deviceId">
          <el-input
            v-model="formData.deviceId"
            :placeholder="$t('commandHistory.deviceId')"
            class="edit-form-default"
            clearable
          />
        </el-form-item>
        <el-form-item :label="$t('commandHistory.commandCode')" prop="commandCode">
          <el-input
            v-model="formData.commandCode"
            :placeholder="$t('commandHistory.commandCode')"
            class="edit-form-default"
            clearable
          />
        </el-form-item>
        <el-form-item :label="$t('commandHistory.status')" prop="status">
          <el-input
            v-model="formData.status"
            :placeholder="$t('commandHistory.status')"
            class="edit-form-default"
            clearable
          />
        </el-form-item>
      </template>
    </tool-card>

    <blank-card>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <el-table-column :label="$t('commandHistory.recordId')" min-width="180" prop="recordId" show-overflow-tooltip />
        <el-table-column :label="$t('commandHistory.deviceId')" min-width="160" prop="deviceId" show-overflow-tooltip />
        <el-table-column :label="$t('commandHistory.commandCode')" min-width="140" prop="commandCode" />
        <el-table-column :label="$t('commandHistory.status')" prop="status" width="100" />
        <el-table-column
          :label="$t('commandHistory.error')"
          min-width="180"
          prop="errorMessage"
          show-overflow-tooltip
        />
        <el-table-column
          :formatter="timestampColumn"
          :label="$t('commandHistory.occurTime')"
          prop="occurTime"
          width="165"
        />
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

    <el-dialog
      v-model="detailVisible"
      :append-to-body="true"
      :title="$t('commandHistory.detailTitle')"
      draggable
      width="700px"
    >
      <el-descriptions v-if="detailRow" :column="2" border>
        <el-descriptions-item :label="$t('commandHistory.recordId')" :span="2">
          {{ detailRow.recordId }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.deviceId')">{{ detailRow.deviceId }}</el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.commandId')">{{ detailRow.commandId }}</el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.commandCode')">
          {{ detailRow.commandCode }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.status')">{{ detailRow.status }}</el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.errorCode')">
          {{ detailRow.errorCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.errorMessage')">
          {{ detailRow.errorMessage || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.source')">{{ detailRow.source || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.sourceUserId')">
          {{ detailRow.sourceUserId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.paramValues')" :span="2">
          <pre class="json-preview">{{ formatJson(detailRow.paramValues) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.resultValues')" :span="2">
          <pre class="json-preview">{{ formatJson(detailRow.resultValues) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.configSnapshot')" :span="2">
          <pre class="json-preview">{{ formatJson(detailRow.configSnapshot) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.occurTime')">
          {{ timestampLabel(detailRow.occurTime) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.sendTime')">
          {{ timestampLabel(detailRow.sendTime) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.finishTime')">
          {{ timestampLabel(detailRow.finishTime) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('commandHistory.expireTime')">
          {{ timestampLabel(detailRow.expireTime) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('common.createTime')">
          {{ timestampLabel(detailRow.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('common.operationTime')">
          {{ timestampLabel(detailRow.operateTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { getCommandHistoryById, listCommandHistory } from '@/api/command';
  import { usePagedList } from '@/composables/usePagedList';
  import { timestampColumn, timestampLabel } from '@/utils/dateUtil';
  import { prettyJson } from '@/utils/jsonUtil';
  import type { CommandHistory } from '@/config/types';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import { cleanSearchParams, resetSearchForm } from '@/utils/searchParamUtil';

  const {
    state: reactiveData,
    load,
    search,
    reset,
    sizeChange,
    currentChange,
  } = usePagedList<CommandHistory, Record<string, unknown>>({
    request: (query) => listCommandHistory(query),
  });

  const formData = reactive<Record<string, string>>({});
  const detailVisible = ref(false);
  const detailRow = ref<CommandHistory | null>(null);

  const formatJson = (value: unknown) => prettyJson(value);

  const onSearch = (data: Record<string, string>) => {
    search(cleanSearchParams(data));
  };

  const onReset = () => {
    resetSearchForm(formData, {});
    reset();
  };

  const refresh = () => load();

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
