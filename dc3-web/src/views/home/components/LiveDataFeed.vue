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
  <dashboard-card
    v-model:interval="intervalMs"
    :auto-refresh="intervalOptions"
    :empty="!loading && rows.length === 0"
    :empty-text="$t('home.liveFeed.empty')"
    :loading="loading"
    :title="$t('home.liveFeed.title')"
    body-mode="scroll"
    class="live-feed"
    loading-target="none"
    @refresh="refresh"
  >
    <el-timeline>
      <el-timeline-item
        v-for="row in rows"
        :key="rowKey(row)"
        :color="typeColor(row.valueType)"
        :timestamp="formatTime(row.createTime)"
        placement="top"
      >
        <div class="live-feed__item">
          <div class="live-feed__line">
            <span class="live-feed__driver">{{ displayDriver(row) }}</span>
            <span class="live-feed__sep">/</span>
            <span class="live-feed__device">{{ displayDevice(row) }}</span>
            <span class="live-feed__sep">/</span>
            <span class="live-feed__point">{{ displayPoint(row) }}</span>
          </div>
          <div class="live-feed__value-line">
            <span :class="`live-feed__tag--${row.valueType?.toLowerCase() || 'string'}`" class="live-feed__tag">
              {{ row.valueType || 'STR' }}
            </span>
            <span class="live-feed__value">{{ row.calValue ?? row.rawValue ?? '-' }}</span>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>

    <template #footer>
      <span v-if="lastRefreshed">{{ $t('home.liveFeed.updatedAt', {time: formatTime(lastRefreshed)}) }}</span>
      <span v-else>-</span>
      <span>{{ $t('home.liveFeed.rows', {n: rows.length}) }}</span>
    </template>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import {computed, onMounted, ref} from 'vue';
  import {useI18n} from 'vue-i18n';

  import {streamLatest} from '@/api/dashboard';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';

  interface Row {
    deviceId: number | string;
    pointId: number | string;
    driverId?: number | string;
    // driverName / deviceName / pointName are populated server-side by
    // DashboardServiceImpl.latestStream via the metadata facades, so the
    // feed can render the full tuple without a separate lookup round-trip.
    driverName?: string;
    deviceName?: string;
    pointName?: string;
    rawValue?: string;
    calValue?: string;
    valueType?: string;
    createTime?: string | Date;
  }

  const props = defineProps({
    size: {type: Number, default: 20},
  });

  const {t} = useI18n();

  const loading = ref(false);
  const rows = ref<Row[]>([]);
  const lastRefreshed = ref<string>('');
  const intervalMs = ref(0);

  const intervalOptions = computed(() => [
    {label: t('home.liveFeed.intervalOff'), value: 0},
    {label: '5s', value: 5000},
    {label: '10s', value: 10000},
    {label: '30s', value: 30000},
  ]);

  const refresh = async () => {
    loading.value = true;
    try {
      const res: any = await streamLatest(props.size);
      const data: Row[] = res?.data ?? [];
      rows.value = data;
      lastRefreshed.value = new Date().toISOString();
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  // Show the name when the server resolved it, otherwise fall back to the
  // raw ID so deleted-but-historical rows remain legible.
  const displayDriver = (r: Row) =>
    r.driverName || (r.driverId && String(r.driverId) !== '0' ? String(r.driverId) : '-');
  const displayDevice = (r: Row) => r.deviceName || String(r.deviceId);
  const displayPoint = (r: Row) => r.pointName || String(r.pointId);

  const rowKey = (r: Row) => `${r.deviceId}-${r.pointId}-${r.createTime}`;

  const formatTime = (v?: string | Date) => {
    if (!v) return '';
    const d = typeof v === 'string' ? new Date(v.replace(' ', 'T')) : v;
    if (Number.isNaN(d.getTime())) return String(v);
    return d.toLocaleTimeString('zh-CN', {hour12: false});
  };

  const typeColor = (vt?: string) => {
    const t = (vt || '').toLowerCase();
    if (t === 'int' || t === 'long') return '#409eff';
    if (t === 'float' || t === 'double') return '#67c23a';
    if (t === 'bool') return '#e6a23c';
    if (t === 'json') return '#9059f6';
    // STRING / unknown — pick the neutral accent over default gray so the
    // timeline dot still reads as "live data" instead of a placeholder.
    return '#409eff';
  };

  onMounted(refresh);
</script>

<style lang="scss" scoped>
  .live-feed {
    :deep(.el-timeline) {
      padding: 8px 16px 0;
    }

    :deep(.el-timeline-item__wrapper) {
      padding-left: 16px;
    }

    :deep(.el-timeline-item__timestamp) {
      font-size: 11px;
    }

    // Tint the timeline skeleton: connecting line, default node, and the
    // ring around coloured nodes get the same cool-blue accent so the
    // feed reads as one continuous stream instead of disconnected dots
    // on a gray spine.
    :deep(.el-timeline-item__tail) {
      border-left-color: rgba(64, 158, 255, 0.25);
    }

    :deep(.el-timeline-item__node) {
      box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
    }

    .live-feed__item {
      padding-bottom: 4px;
    }

    .live-feed__line {
      font-size: 13px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    // Per-dimension accent colours so the driver / device / point
    // hierarchy is readable at a glance without falling back to <el-tag>
    // chrome. Palette reuses the StatCard tones (driver=purple "promotion",
    // device=blue "management", point=green "data") so the same entity
    // reads the same colour everywhere on the page.
    .live-feed__driver {
      color: #9059f6;
      font-weight: 500;
    }

    .live-feed__device {
      color: #409eff;
      font-weight: 600;
    }

    .live-feed__point {
      color: #67c23a;
      font-weight: 500;
    }

    .live-feed__sep {
      color: #dcdfe6;
    }

    .live-feed__value-line {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 4px;
    }

    .live-feed__tag {
      font-size: 10px;
      font-weight: 600;
      padding: 1px 5px;
      border-radius: 4px;
      white-space: nowrap;

      &--int,
      &--long {
        background: rgba(64, 158, 255, 0.1);
        color: #409eff;
      }

      &--float,
      &--double {
        background: rgba(103, 194, 58, 0.1);
        color: #67c23a;
      }

      &--bool {
        background: rgba(230, 162, 60, 0.1);
        color: #e6a23c;
      }

      &--json {
        background: rgba(144, 89, 246, 0.1);
        color: #9059f6;
      }
    }

    .live-feed__value {
      font-family: 'Menlo', monospace;
      color: #303133;
      font-weight: 500;
      font-size: 12px;
    }
  }
</style>
