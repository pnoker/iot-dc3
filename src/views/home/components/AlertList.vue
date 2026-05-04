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
  <el-card class="alert-list" shadow="never">
    <template #header>
      <div class="alert-list__header">
        <span class="alert-list__title">
          {{ $t('home.alertList.title') }}
          <!-- Badge mirrors the actual rendered list length so the badge
               number and what the user sees below it never drift apart. -->
          <el-badge v-if="rows.length > 0" :value="rows.length" :max="99" type="danger" class="alert-list__badge" />
        </span>
        <el-button :icon="Refresh" :loading="loading" circle size="small" @click="refresh" />
      </div>
    </template>

    <div v-if="!loading && rows.length === 0" class="alert-list__empty">
      <el-empty :description="$t('home.alertList.empty')" :image-size="80" />
    </div>
    <div v-for="group in groupedRows" :key="group.date" class="alert-list__group">
      <div class="alert-list__date">{{ group.date }}</div>
      <el-timeline>
        <el-timeline-item
          v-for="row in group.items"
          :key="row.id"
          :timestamp="formatClock(row.createTime)"
          :type="timelineColour(row.eventTypeFlag)"
          :hollow="row.confirmFlag === 1"
          placement="top"
        >
          <div class="alert-list__body">
            <div class="alert-list__tags">
              <el-tag :type="tagType(row.eventTypeFlag)" size="small">
                {{ levelLabel(row.eventTypeFlag) }}
              </el-tag>
              <el-tag :type="row.source === 'device' ? 'primary' : 'warning'" size="small">
                {{ sourceLabel(row) }}
              </el-tag>
              <span class="alert-list__name">{{ nameFor(row) }}</span>
              <el-tag v-if="row.confirmFlag === 1" type="success" size="small" effect="plain">
                {{ $t('common.confirmed') }}
              </el-tag>
            </div>
            <div v-if="row.message" class="alert-list__message" :title="row.message">{{ row.message }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { Refresh } from '@element-plus/icons-vue';
  import { useI18n } from 'vue-i18n';

  import { alertLatest, alertStats } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getDriverByIds } from '@/api/driver';

  interface AlertRow {
    id: number | string;
    source: 'device' | 'driver';
    sourceId: number | string;
    pointId: number | string;
    eventTypeFlag: number;
    confirmFlag: number;
    createTime: string;
    message?: string;
  }

  const props = defineProps({
    size: { type: Number, default: 10 },
  });

  const { t } = useI18n();

  const loading = ref(false);
  const rows = ref<AlertRow[]>([]);
  const stats = reactive({ total: 0, unconfirmed: 0 });
  const deviceMap = reactive<Record<string, string>>({});
  const driverMap = reactive<Record<string, string>>({});

  const refresh = async () => {
    loading.value = true;
    try {
      const [s, l]: any = await Promise.all([alertStats(), alertLatest(props.size)]);
      stats.total = (s?.data?.driverAlerts ?? 0) + (s?.data?.deviceAlerts ?? 0);
      stats.unconfirmed = (s?.data?.driverUnconfirmed ?? 0) + (s?.data?.deviceUnconfirmed ?? 0);
      const data: AlertRow[] = l?.data ?? [];
      rows.value = data;
      await resolveNames(data);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  const resolveNames = async (batch: AlertRow[]) => {
    const devIds = Array.from(
      new Set(
        batch
          .filter((r) => r.source === 'device')
          .map((r) => String(r.sourceId))
          .filter((id) => id && !deviceMap[id])
      )
    );
    const drvIds = Array.from(
      new Set(
        batch
          .filter((r) => r.source === 'driver')
          .map((r) => String(r.sourceId))
          .filter((id) => id && !driverMap[id])
      )
    );
    const jobs: Promise<void>[] = [];
    if (devIds.length) {
      jobs.push(
        getDeviceByIds(devIds)
          .then((r: any) => {
            const d = r?.data || {};
            for (const id of devIds) {
              if (d[id]) deviceMap[id] = d[id].deviceName || id;
            }
          })
          .catch(() => {})
      );
    }
    if (drvIds.length) {
      jobs.push(
        getDriverByIds(drvIds)
          .then((r: any) => {
            const d = r?.data || {};
            for (const id of drvIds) {
              if (d[id]) driverMap[id] = d[id].driverName || id;
            }
          })
          .catch(() => {})
      );
    }
    await Promise.all(jobs);
  };

  // Group the flat row list by YYYY-MM-DD so the timeline has day headings.
  const groupedRows = computed(() => {
    const byDate = new Map<string, AlertRow[]>();
    for (const row of rows.value) {
      const d = parseTime(row.createTime);
      const key = d ? d.toLocaleDateString() : '-';
      if (!byDate.has(key)) byDate.set(key, []);
      byDate.get(key)!.push(row);
    }
    // Map preserves insertion order; rows come back newest-first so date
    // groups also land newest-first without extra sorting.
    return Array.from(byDate.entries()).map(([date, items]) => ({ date, items }));
  });

  const sourceLabel = (r: AlertRow) =>
    r.source === 'device' ? t('home.alertList.sourceDevice') : t('home.alertList.sourceDriver');

  const nameFor = (r: AlertRow) => {
    const id = String(r.sourceId);
    return r.source === 'device' ? deviceMap[id] || id : driverMap[id] || id;
  };

  const levelLabel = (flag: number) => {
    switch (flag) {
      case 2:
      case 3:
        return t('common.levelError');
      case 1:
        return t('common.levelWarn');
      default:
        return t('common.levelInfo');
    }
  };

  const tagType = (flag: number): 'info' | 'warning' | 'danger' => {
    if (flag >= 2) return 'danger';
    if (flag === 1) return 'warning';
    return 'info';
  };

  const timelineColour = (flag: number): 'primary' | 'warning' | 'danger' | 'info' => {
    if (flag >= 2) return 'danger';
    if (flag === 1) return 'warning';
    return 'primary';
  };

  const parseTime = (v?: string): Date | null => {
    if (!v) return null;
    const d = new Date(v.replace(' ', 'T'));
    return Number.isNaN(d.getTime()) ? null : d;
  };

  const formatClock = (v?: string) => {
    const d = parseTime(v);
    if (!d) return v || '';
    return d.toLocaleTimeString('zh-CN', { hour12: false });
  };

  onMounted(refresh);
  defineExpose({ refresh });
</script>

<style lang="scss" scoped>
  .alert-list {
    min-height: 300px;
    height: 100%;
    display: flex;
    flex-direction: column;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    :deep(.el-card__body) {
      // Same pattern as RecentUnconfirmed: body is the scroll container
      // (native overflow:auto), content flows inside. Native overflow
      // works reliably with flex:1 sizing where el-scrollbar's height
      // prop silently degraded to auto and blew the card out.
      flex: 1;
      padding: 0;
      min-height: 0;
      overflow: auto;
    }

    .alert-list__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .alert-list__title {
      font-weight: 600;
      color: #303133;
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }

    .alert-list__badge {
      :deep(.el-badge__content) {
        transform: none;
        position: static;
      }
    }

    .alert-list__group {
      padding: 10px 20px 0;

      &:last-child {
        padding-bottom: 10px;
      }
    }

    .alert-list__date {
      font-size: 12px;
      font-weight: 600;
      color: #909399;
      padding: 4px 0 6px;
      border-bottom: 1px dashed var(--el-border-color-lighter);
      margin-bottom: 8px;
    }

    :deep(.el-timeline) {
      padding-left: 4px;
    }

    :deep(.el-timeline-item__timestamp) {
      font-size: 12px;
      color: #909399;
      margin-bottom: 2px;
    }

    .alert-list__body {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .alert-list__tags {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
    }

    .alert-list__name {
      font-size: 13px;
      color: #606266;
    }

    .alert-list__message {
      font-size: 12px;
      color: #909399;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      word-break: break-word;
    }

    .alert-list__empty {
      padding: 40px 0;
    }
  }
</style>
