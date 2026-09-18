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
        <el-form-item :label="$t('eventHistory.deviceId')" prop="deviceId">
          <el-input
            v-model="formData.deviceId"
            :placeholder="$t('eventHistory.deviceId')"
            class="edit-form-default"
            clearable
          />
        </el-form-item>
        <el-form-item :label="$t('eventHistory.eventCode')" prop="eventCode">
          <el-input
            v-model="formData.eventCode"
            :placeholder="$t('eventHistory.eventCode')"
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
        <el-button link type="primary" @click="openDetail(row as EventHistoryRecord)">
          {{ $t('common.detail') }}
        </el-button>
      </template>
    </responsive-record-list>

    <el-dialog
      v-model="detailVisible"
      :append-to-body="true"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      :title="$t('eventHistory.detailTitle')"
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
        <el-descriptions-item :label="$t('eventHistory.recordId')" :span="2">
          {{ detailRow.recordId }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.deviceId')">{{ detailRow.deviceId }}</el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.eventId')">{{ detailRow.eventId }}</el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.eventCode')">{{ detailRow.eventCode }}</el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.type')"
        >{{ eventTypeLabel(detailRow.eventTypeFlag) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.level')"
        >{{ eventLevelLabel(detailRow.eventLevelFlag) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.message')" :span="2">
          {{ detailRow.message || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.paramValues')" :span="2">
          <pre class="json-preview">{{ formatJson(detailRow.paramValues) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.configSnapshot')" :span="2">
          <pre class="json-preview">{{ formatJson(detailRow.configSnapshot) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.occurTime')">
          {{ timestampLabel(detailRow.occurTime) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.receiveTime')">
          {{ timestampLabel(detailRow.receiveTime) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.acknowledgeFlag')">
          {{ detailRow.acknowledgeFlag }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.acknowledgeUserId')">
          {{ detailRow.acknowledgeUserId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('eventHistory.acknowledgeTime')">
          {{ timestampLabel(detailRow.acknowledgeTime) }}
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
import {listDeviceByIds} from '@/api/device';
import {getEventHistoryByRecordId, listEventHistory} from '@/api/event';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import {useBreakpoint} from '@/composables/useBreakpoint';
import {usePagedList} from '@/composables/usePagedList';
import {timestampLabel} from '@/utils/dateUtil';
import {prettyJson} from '@/utils/jsonUtil';
import {eventLevelLabel, eventLevelTag, eventTypeLabel} from '@/utils/thingModelFormatUtil';
import type {EventHistoryRecord, ResponsiveListColumn} from '@/config/types';
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
} = usePagedList<EventHistoryRecord, Record<string, unknown>>({
  request: (query) => listEventHistory(query),
});

const formData = reactive<Record<string, string>>({});
const detailVisible = ref(false);
const detailRow = ref<EventHistoryRecord | null>(null);
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
const resolveDeviceNames = async (rows: EventHistoryRecord[]) => {
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
  (rows) => resolveDeviceNames((rows as EventHistoryRecord[]) || []),
  {
    immediate: true,
  }
);
const deviceNameFor = (row: EventHistoryRecord) => deviceNameMap[String(row.deviceId)] || String(row.deviceId ?? '-');

const columns = computed<ResponsiveListColumn<EventHistoryRecord>[]>(() => [
  {
    key: 'device',
    label: t('eventHistory.device'),
    minWidth: 160,
    mobile: 'primary',
    formatter: deviceNameFor,
  },
  {key: 'eventCode', label: t('eventHistory.eventCode'), minWidth: 140},
  {
    key: 'eventTypeFlag',
    label: t('eventHistory.type'),
    width: 110,
    formatter: (row) => eventTypeLabel(row.eventTypeFlag),
  },
  {
    key: 'eventLevelFlag',
    label: t('eventHistory.level'),
    width: 100,
    kind: 'tag',
    formatter: (row) => eventLevelLabel(row.eventLevelFlag),
    tagType: (row) => eventLevelTag(row.eventLevelFlag),
  },
  {key: 'acknowledgeFlag', label: t('eventHistory.ack'), width: 90},
  {key: 'message', label: t('eventHistory.message'), minWidth: 200},
  {key: 'occurTime', label: t('eventHistory.occurTime'), width: 165, kind: 'time'},
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

const loadDetail = async (row: EventHistoryRecord) => {
  const requestId = ++detailRequestId;
  detailLoading.value = true;
  detailError.value = false;
  try {
    const response = await getEventHistoryByRecordId(row.recordId);
    if (requestId === detailRequestId && detailVisible.value) detailRow.value = response || row;
  } catch {
    if (requestId === detailRequestId && detailVisible.value) detailError.value = true;
  } finally {
    if (requestId === detailRequestId) detailLoading.value = false;
  }
};

const openDetail = (row: EventHistoryRecord) => {
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
