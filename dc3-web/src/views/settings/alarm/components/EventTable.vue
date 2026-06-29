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
          <el-segmented
            v-model="fd.alarmTypeFlag"
            :options="[{label: $t('common.all'), value: ''}, ...alarmTypeOptions]"
          />
        </el-form-item>
        <el-form-item :label="$t('settings.event.confirmFlag')" prop="confirmFlag">
          <el-segmented
            v-model="fd.confirmFlag"
            :options="[
              {label: $t('common.all'), value: ''},
              {label: $t('common.unconfirmed'), value: 0},
              {label: $t('common.confirmed'), value: 1},
            ]"
          />
        </el-form-item>
        <el-form-item :label="$t('settings.event.timeRange')" prop="rangeKey">
          <range-segmented v-model="fd.rangeKey" include-all />
        </el-form-item>
      </template>
      <template v-if="selection.length > 0" #actions>
        <el-popconfirm
          :cancel-button-text="$t('common.cancel')"
          :confirm-button-text="$t('common.confirm')"
          :title="$t('settings.event.bulkConfirmTitle', {n: selection.length})"
          @confirm="bulkConfirm(true)"
        >
          <template #reference>
            <el-button :loading="bulkRunning" type="primary">
              {{ $t('settings.event.bulkConfirm', {n: selection.length}) }}
            </el-button>
          </template>
        </el-popconfirm>
        <el-popconfirm
          :cancel-button-text="$t('common.cancel')"
          :confirm-button-text="$t('common.confirm')"
          :title="$t('settings.event.bulkUnconfirmTitle', {n: selection.length})"
          @confirm="bulkConfirm(false)"
        >
          <template #reference>
            <el-button :loading="bulkRunning">
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

    <blank-card>
      <el-table
        v-loading="loading"
        :data="rows"
        :row-key="(row: Row) => `${row.source}:${row.id}`"
        class="settings-table"
        stripe
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="44" />
        <!-- @vue-generic {Row} -->
        <el-table-column :label="entityLabel" min-width="180" show-overflow-tooltip>
          <template #default="{row}">
            <span>{{ nameFor(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="source === 'point'" :label="$t('settings.event.sourceId')" prop="sourceId" width="140" />
        <el-table-column
          v-if="source === 'device' || source === 'point'"
          :label="$t('settings.event.pointId')"
          prop="pointId"
          width="140"
        />
        <el-table-column :label="$t('settings.event.alarmType')" width="110">
          <template #default="{row}">
            <el-tag :type="alarmTypeTag(row.eventTypeFlag)" size="small">
              {{ alarmTypeLabel(row.eventTypeFlag) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('settings.event.alarmLevel')" width="100">
          <template #default="{row}">
            <el-tag :type="alarmLevelTag(row.alarmLevelFlag)" size="small">
              {{ alarmLevelLabel(row.alarmLevelFlag) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('settings.event.message')" min-width="240" prop="message" show-overflow-tooltip />
        <el-table-column :label="$t('settings.event.confirmFlag')" width="110">
          <template #default="{row}">
            <el-tag :type="row.confirmFlag === 'CONFIRMED' ? 'success' : 'warning'" size="small">
              {{ row.confirmFlag === 'CONFIRMED' ? $t('common.confirmed') : $t('common.unconfirmed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          :formatter="timestampColumn"
          :label="$t('settings.event.createTime')"
          prop="createTime"
          width="180"
        />
        <!-- @vue-generic {Row} -->
        <el-table-column :label="$t('common.operation')" fixed="right" width="140">
          <template #default="{row}">
            <el-popconfirm
              v-if="row.confirmFlag !== 'CONFIRMED'"
              :cancel-button-text="$t('common.cancel')"
              :confirm-button-text="$t('common.confirm')"
              :title="$t('settings.event.confirmTitle')"
              @confirm="confirmRow(row)"
            >
              <template #reference>
                <el-button link type="primary">{{ $t('settings.event.confirm') }}</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              v-else
              :cancel-button-text="$t('common.cancel')"
              :confirm-button-text="$t('common.confirm')"
              :title="$t('settings.event.unconfirmTitle')"
              @confirm="unconfirmRow(row)"
            >
              <template #reference>
                <el-button link type="warning">{{ $t('settings.event.unconfirm') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="$t('settings.event.empty')" />
        </template>
      </el-table>
    </blank-card>
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
  import {timestampColumn} from '@/utils/dateUtil';
  import {successMessage} from '@/utils/notificationUtil';
  import {
    ALARM_TYPE_OPTIONS,
    alarmLevelLabel,
    alarmLevelTag,
    alarmTypeLabel,
    alarmTypeTag,
  } from '@/utils/thingModelFormatUtil';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import type {RangeKey} from '@/components/segmented/RangeSegmented.vue';
  import RangeSegmented from '@/components/segmented/RangeSegmented.vue';

  interface Row {
    id: number | string;
    source: 'point' | 'device' | 'driver';
    sourceId: number | string;
    pointId: number | string;
    eventTypeFlag: number;
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
  const bulkRunning = ref(false);
  const autoRefreshTimer = ref<ReturnType<typeof setInterval> | null>(null);
  const lastRefreshTime = ref<number>(Date.now());
  const AUTO_REFRESH_INTERVAL = 30000;

  const lastRefreshText = computed(() => {
    const d = new Date(lastRefreshTime.value);
    return d.toLocaleTimeString();
  });
  const selection = ref<Row[]>([]);
  const rows = ref<Row[]>([]);
  const nameMap = reactive<Record<string, string>>({});

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
  const formData = reactive<{alarmTypeFlag: number | ''; confirmFlag: number | ''; rangeKey: RangeKey}>({
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

  const load = async () => {
    loading.value = true;
    try {
      const res: any = await alertPage({
        source: props.source,
        eventTypeFlag: formData.alarmTypeFlag === '' ? null : Number(formData.alarmTypeFlag),
        confirmFlag: formData.confirmFlag === '' ? null : Number(formData.confirmFlag),
        rangeKey: formData.rangeKey || null,
        current: page.current,
        size: page.size,
      });
      const data = res?.data ?? {};
      rows.value = data.records ?? [];
      page.total = Number(data.total ?? 0);
      await resolveNames(rows.value);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
      lastRefreshTime.value = Date.now();
    }
  };

  const resolveNames = async (batch: Row[]) => {
    const ids = Array.from(new Set(batch.map((r) => String(r.sourceId)).filter((id) => id && !nameMap[id])));
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
      const data = res?.data || {};
      for (const id of ids) {
        const item = data[id];
        if (item) {
          if (props.source === 'point') {
            nameMap[id] = item.pointName || id;
          } else if (props.source === 'device') {
            nameMap[id] = item.deviceName || id;
          } else {
            nameMap[id] = item.driverName || id;
          }
        }
      }
    } catch {
      // handled globally
    }
  };

  const nameFor = (r: Row) => nameMap[String(r.sourceId)] || String(r.sourceId);

  const onSearch = () => {
    page.current = 1;
    load();
  };

  const onReset = () => {
    formData.alarmTypeFlag = '';
    formData.confirmFlag = '';
    formData.rangeKey = '';
    page.current = 1;
    load();
  };

  const sizeChange = (v: number) => {
    page.size = v;
    page.current = 1;
    load();
  };

  const currentChange = (v: number) => {
    page.current = v;
    load();
  };

  const confirmRow = async (row: Row) => {
    try {
      await alertConfirm(row.source, row.id);
      successMessage();
      load();
    } catch {
      // handled globally
    }
  };

  const unconfirmRow = async (row: Row) => {
    try {
      await alertUnconfirm(row.source, row.id);
      successMessage();
      load();
    } catch {
      // handled globally
    }
  };

  const onSelectionChange = (selected: Row[]) => {
    selection.value = selected;
  };

  const bulkConfirm = async (confirm: boolean) => {
    if (selection.value.length === 0) return;
    bulkRunning.value = true;
    try {
      const items = selection.value.map((r) => ({source: r.source, id: r.id}));
      await alertBulkConfirm(items, confirm);
      successMessage();
      selection.value = [];
      load();
    } catch {
      // handled globally
    } finally {
      bulkRunning.value = false;
    }
  };

  watch(
    () => props.source,
    () => {
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
    if (autoRefreshTimer.value) {
      clearInterval(autoRefreshTimer.value);
      autoRefreshTimer.value = null;
    }
  });

  load();
</script>

<style lang="scss" scoped>
  .settings-table__sub {
    margin-left: 6px;
    color: #909399;
    font-size: 12px;
  }

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
