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
      :page="page"
      hide-sort
      @refresh="load"
      @reset="onReset"
      @search="onSearch"
      @size-change="sizeChange"
      @current-change="currentChange"
    >
      <template #filters="{formData: fd}">
        <el-form-item :label="$t('settings.event.alarmType')" prop="alarmTypeFlag">
          <el-select v-model="fd.alarmTypeFlag" class="event-filter-select">
            <el-option :label="$t('common.all')" value="" />
            <el-option
              v-for="option in alarmTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('settings.event.confirmFlag')" prop="confirmFlag">
          <el-select v-model="fd.confirmFlag" class="event-filter-select">
            <el-option :label="$t('common.all')" value="" />
            <el-option :label="$t('common.unconfirmed')" :value="0" />
            <el-option :label="$t('common.confirmed')" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('settings.event.timeRange')" prop="rangeKey">
          <range-segmented v-model="fd.rangeKey" include-all select/>
        </el-form-item>
      </template>
      <template v-if="selection.length > 0" #actions>
        <el-popconfirm
          :disabled="bulkRunning"
          :cancel-button-text="$t('common.cancel')"
          :confirm-button-text="$t('common.confirm')"
          :title="$t('settings.event.bulkConfirmTitle', {n: selection.length})"
          @confirm="bulkConfirm(true)"
        >
          <template #reference>
            <el-button :disabled="bulkRunning" :loading="bulkRunning" type="primary">
              {{ $t('settings.event.bulkConfirm', {n: selection.length}) }}
            </el-button>
          </template>
        </el-popconfirm>
        <el-popconfirm
          :disabled="bulkRunning"
          :cancel-button-text="$t('common.cancel')"
          :confirm-button-text="$t('common.confirm')"
          :title="$t('settings.event.bulkUnconfirmTitle', {n: selection.length})"
          @confirm="bulkConfirm(false)"
        >
          <template #reference>
            <el-button :disabled="bulkRunning" :loading="bulkRunning">
              {{ $t('settings.event.bulkUnconfirm') }}
            </el-button>
          </template>
        </el-popconfirm>
      </template>
    </tool-card>

    <div class="auto-refresh-bar">
      <span class="auto-refresh-bar__label">{{ $t('common.autoRefresh') }} (30s)</span>
      <span class="auto-refresh-bar__time">{{ $t('common.lastRefreshTime') }}: {{ lastRefreshText }}</span>
    </div>

    <responsive-record-list
      :columns="columns"
      :empty-text="$t('settings.event.empty')"
      :loading="loading"
      :rows="rows"
      :selected-rows="selection"
      :selection-disabled="(row) => bulkRunning || isRowRunning(row)"
      :status="status"
      operation-width="140"
      selectable
      :row-key="rowKey"
      @retry="load"
      @selection-change="onSelectionChange"
    >
      <template #cell-alarmTypeFlag="{row}">
        <el-tag :type="alarmTypeTag(row.alarmTypeFlag)" size="small">
          {{ alarmTypeLabel(row.alarmTypeFlag) }}
        </el-tag>
      </template>
      <template #cell-alarmLevelFlag="{row}">
        <el-tag :type="alarmLevelTag(row.alarmLevelFlag)" size="small">
          {{ alarmLevelLabel(row.alarmLevelFlag) }}
        </el-tag>
      </template>
      <template #cell-confirmFlag="{row}">
        <el-tag :type="row.confirmFlag === 'CONFIRMED' ? 'success' : 'warning'" size="small">
          {{ row.confirmFlag === 'CONFIRMED' ? $t('common.confirmed') : $t('common.unconfirmed') }}
        </el-tag>
      </template>
      <template #actions="{row}">
        <el-popconfirm
          v-if="row.confirmFlag !== 'CONFIRMED'"
          :disabled="bulkRunning || isRowRunning(row)"
          :cancel-button-text="$t('common.cancel')"
          :confirm-button-text="$t('common.confirm')"
          :title="$t('settings.event.confirmTitle')"
          @confirm="confirmRow(row)"
        >
          <template #reference>
            <el-button
              :disabled="bulkRunning || isRowRunning(row)"
              :loading="isRowRunning(row)"
              link
              type="primary"
            >{{ $t('settings.event.confirm') }}</el-button>
          </template>
        </el-popconfirm>
        <el-popconfirm
          v-else
          :disabled="bulkRunning || isRowRunning(row)"
          :cancel-button-text="$t('common.cancel')"
          :confirm-button-text="$t('common.confirm')"
          :title="$t('settings.event.unconfirmTitle')"
          @confirm="unconfirmRow(row)"
        >
          <template #reference>
            <el-button
              :disabled="bulkRunning || isRowRunning(row)"
              :loading="isRowRunning(row)"
              link
              type="warning"
            >{{ $t('settings.event.unconfirm') }}</el-button>
          </template>
        </el-popconfirm>
      </template>
    </responsive-record-list>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRoute} from 'vue-router';

import {alertBulkConfirm, alertConfirm, alertPage, alertUnconfirm} from '@/api/dashboard';
import {listDeviceByIds} from '@/api/device';
import {listDriverByIds} from '@/api/driver';
import {listPointByIds} from '@/api/point';
import {timestampLabel} from '@/utils/dateUtil';
import {successMessage} from '@/utils/notificationUtil';
import {
  ALARM_TYPE_OPTIONS,
  alarmLevelLabel,
  alarmLevelTag,
  alarmTypeLabel,
  alarmTypeTag,
} from '@/utils/thingModelFormatUtil';
import {AUTO_REFRESH_INTERVAL} from '@/config/constant/ui';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import ToolCard from '@/components/card/tool/ToolCard.vue';
import type {ResponsiveListColumn} from '@/config/types';
import type {RangeKey} from '@/config/types/dashboard';
import RangeSegmented from '@/components/segmented/RangeSegmented.vue';

interface Row {
  id: string;
  source: 'point' | 'device' | 'driver';
  sourceId: string;
  pointId: string;
  alarmTypeFlag: number;
  alarmLevelFlag?: number;
  confirmFlag: string;
  createTime: string;
  message: string;
}

const props = defineProps<{
  source: 'point' | 'device' | 'driver';
}>();

const {t} = useI18n();
const route = useRoute();

const loading = ref(false);
const status = ref<'idle' | 'loading' | 'success' | 'error'>('idle');
const bulkRunning = ref(false);
const autoRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null);
const lastRefreshTime = ref<number | null>(null);

const lastRefreshText = computed(() => {
  if (!lastRefreshTime.value) return '-';
  const d = new Date(lastRefreshTime.value);
  return d.toLocaleTimeString();
});
const selection = ref<Row[]>([]);
const rows = ref<Row[]>([]);
const nameMap = reactive<Record<string, string>>({});
const runningRows = ref(new Set<string>());
let loadSequence = 0;

const alarmTypeOptions = ALARM_TYPE_OPTIONS;

const readQuery = () => {
  const q = route.query;
  const parseEnum = (v: unknown, pool: readonly (number | '')[]): number | '' => {
    if (v == null) return '';
    const n = Number(v);
    return pool.includes(n as number) ? (n as number) : '';
  };
  const rangeCandidates = ['', 'today', '24h', '7d', '30d'] as const;
  const rawRange = typeof q.rangeKey === 'string' ? q.rangeKey : '';
  const rangeKey = (rangeCandidates as readonly string[]).includes(rawRange) ? (rawRange as RangeKey) : '';
  return {
    alarmTypeFlag: parseEnum(q.alarmTypeFlag, [0, 1, 2, 3, 4]) as number | '',
    confirmFlag: parseEnum(q.confirmFlag, [0, 1]) as number | '',
    rangeKey,
  };
};

const initial = readQuery();
const formData = reactive<{ alarmTypeFlag: number | ''; confirmFlag: number | ''; rangeKey: RangeKey }>({
  alarmTypeFlag: initial.alarmTypeFlag,
  confirmFlag: initial.confirmFlag,
  rangeKey: initial.rangeKey,
});
const page = reactive({current: 1, size: 20, total: 0});

const entityLabel = computed(() => {
  switch (props.source) {
    case 'point':
      return t('settings.event.sourcePoint');
    case 'device':
      return t('settings.event.sourceDevice');
    case 'driver':
      return t('settings.event.sourceDriver');
    default:
      return '';
  }
});

const rowKey = (row: Row) => `${row.source}:${row.id}`;
const isRowRunning = (row: Row) => runningRows.value.has(rowKey(row));
const nameKey = (source: Row['source'], id: string) => `${source}:${id}`;
const nameFor = (row: Row) => nameMap[nameKey(row.source, String(row.sourceId))] || String(row.sourceId);
const columns = computed<ResponsiveListColumn<Row>[]>(() => [
  {key: 'entityName', label: entityLabel.value, minWidth: 180, mobile: 'primary', formatter: nameFor},
  ...(props.source === 'point'
    ? [{key: 'sourceId', label: t('settings.event.sourceId'), width: 140, mobile: 'hidden' as const}]
    : []),
  ...(props.source === 'device' || props.source === 'point'
    ? [{key: 'pointId', label: t('settings.event.pointId'), width: 140, mobile: 'detail' as const}]
    : []),
  {key: 'alarmTypeFlag', label: t('settings.event.alarmType'), width: 110, kind: 'custom', mobile: 'detail'},
  {key: 'alarmLevelFlag', label: t('settings.event.alarmLevel'), width: 100, kind: 'custom', mobile: 'detail'},
  {key: 'message', label: t('settings.event.message'), minWidth: 240, mobile: 'detail'},
  {key: 'confirmFlag', label: t('settings.event.confirmFlag'), width: 110, kind: 'custom', mobile: 'detail'},
  {
    key: 'createTime',
    label: t('settings.event.createTime'),
    width: 180,
    kind: 'time',
    mobile: 'detail',
    formatter: (row) => timestampLabel(row.createTime),
  },
]);

const load = async () => {
  const sequence = ++loadSequence;
  loading.value = true;
  status.value = 'loading';
  try {
    const res: any = await alertPage({
      source: props.source,
      alarmTypeFlag: formData.alarmTypeFlag === '' ? null : Number(formData.alarmTypeFlag),
      confirmFlag: formData.confirmFlag === '' ? null : Number(formData.confirmFlag),
      rangeKey: formData.rangeKey || null,
      offset: (page.current - 1) * page.size,
      limit: page.size,
    });
    if (sequence !== loadSequence) return;
    const data = res ?? {};
    rows.value = data.items ?? [];
    page.total = Number(data.total ?? 0);
    const visibleKeys = new Set(rows.value.map(rowKey));
    selection.value = selection.value.filter((row) => visibleKeys.has(rowKey(row)));
    await resolveNames(rows.value, sequence);
    if (sequence === loadSequence) {
      status.value = 'success';
      lastRefreshTime.value = Date.now();
    }
  } catch {
    if (sequence === loadSequence) status.value = 'error';
  } finally {
    if (sequence === loadSequence) {
      loading.value = false;
    }
  }
};

const resolveNames = async (batch: Row[], sequence: number) => {
  if (sequence !== loadSequence) return;
  const ids = Array.from(
    new Set(
      batch
        .map((row) => String(row.sourceId))
        .filter((id) => id && !nameMap[nameKey(props.source, id)])
    )
  );
  if (ids.length === 0) return;
  try {
    let res: any;
    if (props.source === 'point') {
      res = await listPointByIds(ids);
    } else if (props.source === 'device') {
      res = await listDeviceByIds(ids);
    } else {
      res = await listDriverByIds(ids);
    }
    const data = res || {};
    if (sequence !== loadSequence) return;
    for (const id of ids) {
      const item = data[id];
      if (item) {
        if (props.source === 'point') {
          nameMap[nameKey(props.source, id)] = item.pointName || id;
        } else if (props.source === 'device') {
          nameMap[nameKey(props.source, id)] = item.deviceName || id;
        } else {
          nameMap[nameKey(props.source, id)] = item.driverName || id;
        }
      }
    }
  } catch {
    // handled globally
  }
};

const onSearch = () => {
  selection.value = [];
  page.current = 1;
  load();
};

const onReset = () => {
  selection.value = [];
  formData.alarmTypeFlag = '';
  formData.confirmFlag = '';
  formData.rangeKey = '';
  page.current = 1;
  load();
};

const sizeChange = (v: number) => {
  selection.value = [];
  page.size = v;
  page.current = 1;
  load();
};

const currentChange = (v: number) => {
  selection.value = [];
  page.current = v;
  load();
};

const confirmRow = async (row: Row) => {
  const key = rowKey(row);
  if (runningRows.value.has(key)) return;
  runningRows.value.add(key);
  try {
    await alertConfirm(row.source, row.id);
    successMessage();
    await load();
  } catch {
    // handled globally
  } finally {
    runningRows.value.delete(key);
  }
};

const unconfirmRow = async (row: Row) => {
  const key = rowKey(row);
  if (runningRows.value.has(key)) return;
  runningRows.value.add(key);
  try {
    await alertUnconfirm(row.source, row.id);
    successMessage();
    await load();
  } catch {
    // handled globally
  } finally {
    runningRows.value.delete(key);
  }
};

const onSelectionChange = (selected: Row[]) => {
  selection.value = selected;
};

const bulkConfirm = async (confirm: boolean) => {
  if (selection.value.length === 0 || bulkRunning.value) return;
  bulkRunning.value = true;
  try {
    const items = selection.value.map((r) => ({source: r.source, id: r.id}));
    await alertBulkConfirm(items, confirm);
    successMessage();
    selection.value = [];
    await load();
  } catch {
    // handled globally
  } finally {
    bulkRunning.value = false;
  }
};

watch(
  () => props.source,
  () => {
    selection.value = [];
    page.current = 1;
    load();
  }
);

watch(
  () => [route.query.rangeKey, route.query.confirmFlag, route.query.alarmTypeFlag],
  () => {
    const next = readQuery();
    formData.alarmTypeFlag = next.alarmTypeFlag;
    formData.confirmFlag = next.confirmFlag;
    formData.rangeKey = next.rangeKey;
    selection.value = [];
    page.current = 1;
    load();
  }
);

onMounted(() => {
  autoRefreshTimer.value = setInterval(() => {
    if (!loading.value && !bulkRunning.value) {
      load();
    }
  }, AUTO_REFRESH_INTERVAL);
});

onBeforeUnmount(() => {
  loadSequence += 1;
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value);
    autoRefreshTimer.value = null;
  }
});

load();
</script>

<style lang="scss" scoped>
.event-filter-select {
  width: 100%;
}

.auto-refresh-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--dc3-space-3);
  padding: var(--dc3-space-1) var(--dc3-space-3);
  margin-bottom: var(--dc3-space-1);
  font-size: var(--el-font-size-extra-small);
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  border-radius: var(--dc3-radius-sm);

  &__label {
    font-weight: 500;
  }

  &__time {
    color: var(--el-text-color-placeholder);
  }
}
</style>
