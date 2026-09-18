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

    <responsive-record-list
      :columns="columns"
      :loading="reactiveData.loading"
      :rows="reactiveData.listData"
      :status="reactiveData.status"
      operation-width="100"
      row-key="recordId"
      @retry="refresh"
    >
      <template #actions="{row}">
        <el-button link type="primary" @click="openDetail(row as CommandHistoryRecord)">
          {{ $t('common.detail') }}
        </el-button>
      </template>
    </responsive-record-list>

    <el-dialog
      v-model="detailVisible"
      :append-to-body="true"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      :title="$t('commandHistory.detailTitle')"
      class="things-dialog"
      destroy-on-close
      draggable
      width="700px"
      @closed="closeDetail"
    >
      <el-alert
        v-if="detailError"
        :closable="false"
        :title="$t('common.detailLoadFailed')"
        class="history-detail__error"
        show-icon
        type="warning"
      >
        <el-button :loading="detailLoading" link type="warning" @click="retryDetail">
          {{ $t('common.retry') }}
        </el-button>
      </el-alert>
      <el-descriptions
        v-if="detailRow"
        v-loading="detailLoading"
        :aria-busy="detailLoading"
        :column="isMobile ? 1 : 2"
        border
      >
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
import {useI18n} from 'vue-i18n';
import {getCommandHistoryByRecordId, listCommandHistory} from '@/api/command';
import {listDeviceByIds} from '@/api/device';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import {useBreakpoint} from '@/composables/useBreakpoint';
import {usePagedList} from '@/composables/usePagedList';
import {timestampLabel} from '@/utils/dateUtil';
import {prettyJson} from '@/utils/jsonUtil';
import type {CommandHistoryRecord, ResponsiveListColumn} from '@/config/types';
import {AUTO_REFRESH_INTERVAL} from '@/config/constant/ui';
import ToolCard from '@/components/card/tool/ToolCard.vue';
import {cleanSearchParams, resetSearchForm} from '@/utils/searchParamUtil';

const {isMobile} = useBreakpoint();
const {t} = useI18n();

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
const detailLoading = ref(false);
const detailError = ref(false);
const autoRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null);
let detailRequestId = 0;
let deviceLookupRequestId = 0;

const lastRefreshText = computed(() => {
  if (!reactiveData.lastUpdated) return '-';
  return new Date(reactiveData.lastUpdated).toLocaleTimeString();
});

const formatJson = (value: unknown) => prettyJson(value);

// Resolve deviceId → deviceName for the list column, reusing the same
// listDeviceByIds source EventTable uses. Filled as rows arrive.
const deviceNameMap = reactive<Record<string, string>>({});
const resolveDeviceNames = async (rows: CommandHistoryRecord[]) => {
  const requestId = ++deviceLookupRequestId;
  const ids = Array.from(
    new Set(rows.map((r) => String(r.deviceId ?? '')).filter((id) => id && id !== '0' && !deviceNameMap[id]))
  );
  if (!ids.length) return;
  try {
    const res: any = await listDeviceByIds(ids);
    if (requestId !== deviceLookupRequestId) return;
    const data = res || {};
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

const columns = computed<ResponsiveListColumn<CommandHistoryRecord>[]>(() => [
  {
    key: 'device',
    label: t('commandHistory.device'),
    minWidth: 160,
    mobile: 'primary',
    formatter: deviceNameFor,
  },
  {key: 'commandCode', label: t('commandHistory.commandCode'), minWidth: 140},
  {key: 'status', label: t('commandHistory.status'), width: 100, kind: 'tag'},
  {key: 'errorMessage', label: t('commandHistory.error'), minWidth: 180},
  {key: 'occurTime', label: t('commandHistory.occurTime'), width: 165, kind: 'time'},
  {key: 'createTime', label: t('common.createTime'), width: 165, kind: 'time', mobile: 'hidden'},
]);

const onSearch = (data: Record<string, string>) => {
  search(cleanSearchParams(data));
};

const onReset = () => {
  resetSearchForm(formData, {});
  reset();
};

const doRefresh = () => load();

const refresh = () => doRefresh();

const loadDetail = async (row: CommandHistoryRecord) => {
  const requestId = ++detailRequestId;
  detailLoading.value = true;
  detailError.value = false;
  try {
    const response = await getCommandHistoryByRecordId(row.recordId);
    if (requestId === detailRequestId && detailVisible.value) detailRow.value = response || row;
  } catch {
    if (requestId === detailRequestId && detailVisible.value) detailError.value = true;
  } finally {
    if (requestId === detailRequestId) detailLoading.value = false;
  }
};

const openDetail = (row: CommandHistoryRecord) => {
  detailRow.value = row;
  detailVisible.value = true;
  void loadDetail(row);
};

const retryDetail = () => {
  if (detailRow.value) void loadDetail(detailRow.value);
};

const closeDetail = () => {
  detailRequestId += 1;
  detailLoading.value = false;
  detailError.value = false;
  detailRow.value = null;
};

onMounted(() => {
  autoRefreshTimer.value = setInterval(async () => {
    if (!reactiveData.loading) {
      await doRefresh();
    }
  }, AUTO_REFRESH_INTERVAL);
});

onBeforeUnmount(() => {
  detailRequestId += 1;
  deviceLookupRequestId += 1;
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value);
    autoRefreshTimer.value = null;
  }
});

void doRefresh();
</script>

<style lang="scss" scoped>
.auto-refresh-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--dc3-space-3);
  padding: var(--dc3-space-1) var(--dc3-space-3);
  margin-bottom: var(--dc3-space-1);
  font-size: var(--el-font-size-extra-small);
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  border-radius: var(--dc3-radius-sm);

  &__label {
    font-weight: var(--el-font-weight-primary);
  }

  &__time {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--el-text-color-placeholder);
  }
}

.history-detail__error {
  margin-bottom: var(--dc3-space-3);
}
</style>
