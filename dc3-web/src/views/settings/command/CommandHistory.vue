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

    <div class="auto-refresh-bar">
      <span class="auto-refresh-bar__label">{{ $t('common.autoRefresh') }} (30s)</span>
      <span class="auto-refresh-bar__time">{{ $t('common.lastRefreshTime') }}: {{ lastRefreshText }}</span>
    </div>

    <blank-card>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <!-- @vue-generic {CommandHistoryRecord} -->
        <el-table-column :label="$t('commandHistory.device')" min-width="160" show-overflow-tooltip>
          <template #default="{row}">{{ deviceNameFor(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('commandHistory.commandCode')" min-width="140" prop="commandCode"/>
        <el-table-column :label="$t('commandHistory.status')" prop="status" width="100"/>
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
        <el-table-column :formatter="timestampColumn" :label="$t('common.createTime')" prop="createTime" width="165"/>
        <!-- @vue-generic {CommandHistoryRecord} -->
        <el-table-column :label="$t('common.operation')" fixed="right" width="100">
          <template #default="{row}">
            <el-button link type="primary" @click="openDetail(row)">{{ $t('common.detail') }}</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="$t('common.description')"/>
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
import {computed, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue';
import {getCommandHistoryByRecordId, listCommandHistory} from '@/api/command';
import {listDeviceByIds} from '@/api/device';
import {usePagedList} from '@/composables/usePagedList';
import {timestampColumn, timestampLabel} from '@/utils/dateUtil';
import {prettyJson} from '@/utils/jsonUtil';
import type {CommandHistoryRecord} from '@/config/types';
import ToolCard from '@/components/card/tool/ToolCard.vue';
import BlankCard from '@/components/card/blank/BlankCard.vue';
import {cleanSearchParams, resetSearchForm} from '@/utils/searchParamUtil';

const {
  state: reactiveData,
  load,
  search,
  reset,
  sizeChange,
  currentChange,
} = usePagedList<CommandHistoryRecord, Record<string, unknown>>({
  request: (query) => listCommandHistory(query),
});

const formData = reactive<Record<string, string>>({});
const detailVisible = ref(false);
const detailRow = ref<CommandHistoryRecord | null>(null);
const autoRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null);
const lastRefreshTime = ref<number>(Date.now());
const AUTO_REFRESH_INTERVAL = 30000;

const lastRefreshText = computed(() => {
  const d = new Date(lastRefreshTime.value);
  return d.toLocaleTimeString();
});

const formatJson = (value: unknown) => prettyJson(value);

// Resolve deviceId → deviceName for the list column, reusing the same
// listDeviceByIds source EventTable uses. Filled as rows arrive.
const deviceNameMap = reactive<Record<string, string>>({});
const resolveDeviceNames = async (rows: CommandHistoryRecord[]) => {
  const ids = Array.from(
    new Set(rows.map((r) => String(r.deviceId ?? '')).filter((id) => id && id !== '0' && !deviceNameMap[id]))
  );
  if (!ids.length) return;
  try {
    const res: any = await listDeviceByIds(ids);
    const data = res?.data || {};
    ids.forEach((id) => {
      if (data[id]) deviceNameMap[id] = data[id].deviceName || id;
    });
  } catch {
    // handled globally
  }
};
watch(
  () => reactiveData.listData,
  (rows) => resolveDeviceNames((rows as CommandHistoryRecord[]) || []),
  {
    immediate: true,
  }
);
const deviceNameFor = (row: CommandHistoryRecord) =>
  deviceNameMap[String(row.deviceId)] || String(row.deviceId ?? '-');

const onSearch = (data: Record<string, string>) => {
  search(cleanSearchParams(data));
};

const onReset = () => {
  resetSearchForm(formData, {});
  reset();
};

const doRefresh = async () => {
  await load();
  lastRefreshTime.value = Date.now();
};

const refresh = () => doRefresh();

const openDetail = (row: CommandHistoryRecord) => {
  getCommandHistoryByRecordId(row.recordId)
    .then((res) => {
      detailRow.value = res.data || row;
      detailVisible.value = true;
    })
    .catch(() => {
      detailRow.value = row;
      detailVisible.value = true;
    });
};

onMounted(() => {
  autoRefreshTimer.value = setInterval(async () => {
    if (!reactiveData.loading) {
      await doRefresh();
    }
  }, AUTO_REFRESH_INTERVAL);
});

onBeforeUnmount(() => {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value);
    autoRefreshTimer.value = null;
  }
});

doRefresh();
</script>

<style lang="scss" scoped>
.auto-refresh-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 12px;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  border-radius: 4px;

  &__label {
    font-weight: 500;
  }

  &__time {
    color: var(--el-text-color-placeholder);
  }
}
</style>
