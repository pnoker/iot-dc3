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
  <el-card class="live-feed" shadow="never">
    <template #header>
      <div class="live-feed__header">
        <span class="live-feed__title">{{ $t('home.liveFeed.title') }}</span>
        <div class="live-feed__actions">
          <el-segmented
            v-model="intervalMs"
            :options="[
              { label: $t('home.liveFeed.intervalOff'), value: 0 },
              { label: '5s', value: 5000 },
              { label: '10s', value: 10000 },
              { label: '30s', value: 30000 },
            ]"
            size="small"
          />
          <el-button :icon="Refresh" :loading="loading" circle size="small" @click="refresh" />
        </div>
      </div>
    </template>

    <el-scrollbar height="360px">
      <div v-if="!loading && rows.length === 0" class="live-feed__empty">
        <el-empty :description="$t('home.liveFeed.empty')" :image-size="80" />
      </div>
      <div v-for="row in rows" :key="rowKey(row)" class="live-feed__row">
        <div class="live-feed__tag" :class="`live-feed__tag--${row.valueType?.toLowerCase() || 'string'}`">
          {{ row.valueType || 'STR' }}
        </div>
        <div class="live-feed__main">
          <div class="live-feed__line">
            <span class="live-feed__device">
              {{ deviceName(row.deviceId) }}
            </span>
            <span class="live-feed__sep">·</span>
            <span class="live-feed__point">{{ pointName(row.pointId) }}</span>
          </div>
          <div class="live-feed__sub">
            <span class="live-feed__value">{{ row.calValue ?? row.rawValue ?? '-' }}</span>
            <span class="live-feed__time">{{ formatTime(row.createTime) }}</span>
          </div>
        </div>
      </div>
    </el-scrollbar>

    <div class="live-feed__footer">
      <span v-if="lastRefreshed">{{ $t('home.liveFeed.updatedAt', { time: formatTime(lastRefreshed) }) }}</span>
      <span v-else>-</span>
      <span>{{ $t('home.liveFeed.rows', { n: rows.length }) }}</span>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
  import { onMounted, onUnmounted, reactive, ref, watch } from 'vue';
  import { Refresh } from '@element-plus/icons-vue';

  import { streamLatest } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getPointByIds } from '@/api/point';

  interface Row {
    deviceId: number | string;
    pointId: number | string;
    driverId?: number | string;
    rawValue?: string;
    calValue?: string;
    valueType?: string;
    createTime?: string | Date;
  }

  const props = defineProps({
    size: { type: Number, default: 20 },
  });

  const loading = ref(false);
  const rows = ref<Row[]>([]);
  const lastRefreshed = ref<string>('');
  const intervalMs = ref(0);
  const deviceMap = reactive<Record<string, string>>({});
  const pointMap = reactive<Record<string, string>>({});

  let timer: ReturnType<typeof setInterval> | null = null;

  const refresh = async () => {
    loading.value = true;
    try {
      const res: any = await streamLatest(props.size);
      const data: Row[] = res?.data ?? [];
      rows.value = data;
      lastRefreshed.value = new Date().toISOString();
      await resolveNames(data);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  const resolveNames = async (batch: Row[]) => {
    const devIds = Array.from(new Set(batch.map((r) => String(r.deviceId)).filter((id) => id && !deviceMap[id])));
    const pointIds = Array.from(new Set(batch.map((r) => String(r.pointId)).filter((id) => id && !pointMap[id])));
    const jobs: Promise<void>[] = [];
    if (devIds.length) {
      jobs.push(
        getDeviceByIds(devIds)
          .then((r: any) => {
            const data = r?.data || {};
            for (const id of devIds) {
              if (data[id]) deviceMap[id] = data[id].deviceName || id;
            }
          })
          .catch(() => {})
      );
    }
    if (pointIds.length) {
      jobs.push(
        getPointByIds(pointIds)
          .then((r: any) => {
            const data = r?.data || {};
            for (const id of pointIds) {
              if (data[id]) pointMap[id] = data[id].pointName || id;
            }
          })
          .catch(() => {})
      );
    }
    await Promise.all(jobs);
  };

  const deviceName = (id: Row['deviceId']) => deviceMap[String(id)] || String(id);
  const pointName = (id: Row['pointId']) => pointMap[String(id)] || String(id);

  const rowKey = (r: Row) => `${r.deviceId}-${r.pointId}-${r.createTime}`;

  const formatTime = (v?: string | Date) => {
    if (!v) return '';
    const d = typeof v === 'string' ? new Date(v.replace(' ', 'T')) : v;
    if (Number.isNaN(d.getTime())) return String(v);
    return d.toLocaleTimeString('zh-CN', { hour12: false });
  };

  watch(intervalMs, (ms) => {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
    if (ms > 0) {
      timer = setInterval(refresh, ms);
    }
  });

  onMounted(refresh);
  onUnmounted(() => {
    if (timer) clearInterval(timer);
  });
</script>

<style lang="scss" scoped>
  .live-feed {
    border-radius: 10px;
    min-height: 440px;
    height: 100%;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    :deep(.el-card__body) {
      padding: 0;
    }

    .live-feed__header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .live-feed__title {
      font-weight: 600;
      color: #303133;
    }

    .live-feed__actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .live-feed__row {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 10px 16px;
      border-bottom: 1px solid var(--el-border-color-lighter);

      &:last-child {
        border-bottom: 0;
      }
    }

    .live-feed__tag {
      font-size: 10px;
      font-weight: 600;
      padding: 2px 6px;
      border-radius: 4px;
      background: #ecf5ff;
      color: #409eff;
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

    .live-feed__main {
      flex: 1;
      min-width: 0;
    }

    .live-feed__line {
      font-size: 13px;
      color: #303133;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .live-feed__device {
      font-weight: 600;
    }

    .live-feed__sep {
      margin: 0 4px;
      color: #c0c4cc;
    }

    .live-feed__point {
      color: #606266;
    }

    .live-feed__sub {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      color: #909399;
      margin-top: 2px;
    }

    .live-feed__value {
      font-family: 'Menlo', monospace;
      color: #303133;
      font-weight: 500;
    }

    .live-feed__empty {
      padding: 40px 0;
    }

    .live-feed__footer {
      display: flex;
      justify-content: space-between;
      padding: 8px 16px;
      font-size: 12px;
      color: #909399;
      border-top: 1px solid var(--el-border-color-lighter);
      background: #fafafa;
    }
  }
</style>
