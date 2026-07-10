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

    <blank-card>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <!-- @vue-generic {EventHistoryRecord} -->
        <el-table-column :label="$t('eventHistory.device')" min-width="160" show-overflow-tooltip>
          <template #default="{row}">{{ deviceNameFor(row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('eventHistory.eventCode')" min-width="140" prop="eventCode"/>
        <el-table-column :label="$t('eventHistory.type')" width="110">
          <template #default="{row}">{{ eventTypeLabel(row.eventTypeFlag) }}</template>
        </el-table-column>
        <el-table-column :label="$t('eventHistory.level')" width="100">
          <template #default="{row}">
            <el-tag :type="eventLevelTag(row.eventLevelFlag)" size="small">
              {{ eventLevelLabel(row.eventLevelFlag) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('eventHistory.ack')" prop="acknowledgeFlag" width="90"/>
        <el-table-column :label="$t('eventHistory.message')" min-width="200" prop="message" show-overflow-tooltip/>
        <el-table-column
          :formatter="timestampColumn"
          :label="$t('eventHistory.occurTime')"
          prop="occurTime"
          width="165"
        />
        <el-table-column :formatter="timestampColumn" :label="$t('common.createTime')" prop="createTime" width="165"/>
        <!-- @vue-generic {EventHistoryRecord} -->
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
      :title="$t('eventHistory.detailTitle')"
      draggable
      width="700px"
    >
      <el-descriptions v-if="detailRow" :column="2" border>
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
import {listDeviceByIds} from '@/api/device';
import {getEventHistoryByRecordId, listEventHistory} from '@/api/event';
import {usePagedList} from '@/composables/usePagedList';
import {timestampColumn, timestampLabel} from '@/utils/dateUtil';
import {prettyJson} from '@/utils/jsonUtil';
import {eventLevelLabel, eventLevelTag, eventTypeLabel} from '@/utils/thingModelFormatUtil';
import type {EventHistoryRecord} from '@/config/types';
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
} = usePagedList<EventHistoryRecord, Record<string, unknown>>({
  request: (query) => listEventHistory(query),
});

const formData = reactive<Record<string, string>>({});
const detailVisible = ref(false);
const detailRow = ref<EventHistoryRecord | null>(null);
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
const resolveDeviceNames = async (rows: EventHistoryRecord[]) => {
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
  (rows) => resolveDeviceNames((rows as EventHistoryRecord[]) || []),
  {
    immediate: true,
  }
);
const deviceNameFor = (row: EventHistoryRecord) => deviceNameMap[String(row.deviceId)] || String(row.deviceId ?? '-');

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

const openDetail = (row: EventHistoryRecord) => {
  getEventHistoryByRecordId(row.recordId)
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
