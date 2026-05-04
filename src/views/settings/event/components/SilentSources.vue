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
    class="silent-sources"
    :title="t('settings.event.overview.silentTitle')"
    :subtitle="t('settings.event.overview.silentSubtitle', { min: silentKey })"
    :badge="rows.length || null"
    :loading="loading"
    loading-target="button"
    :empty="!loading && rows.length === 0"
    :empty-text="t('settings.event.overview.silentEmpty')"
    :empty-image-size="60"
    body-mode="scroll"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="silentKey" :options="silentOptions" size="small" />
    </template>

    <el-table :data="rows" size="small" @row-click="onRowClick">
      <el-table-column prop="deviceId" :label="t('settings.event.overview.colDevice')" min-width="110">
        <template #default="{ row }">{{ deviceName(row) }}</template>
      </el-table-column>
      <el-table-column prop="pointId" :label="t('settings.event.overview.colPoint')" min-width="110">
        <template #default="{ row }">{{ pointName(row) }}</template>
      </el-table-column>
      <el-table-column prop="lastSeen" :label="t('settings.event.overview.colLastSeen')" min-width="160">
        <template #default="{ row }">{{ formatTime(row.lastSeen) }}</template>
      </el-table-column>
      <el-table-column prop="silentSeconds" :label="t('settings.event.overview.colSilentFor')" min-width="110">
        <template #default="{ row }">
          <el-tag type="warning" size="small">{{ humanDuration(row.silentSeconds) }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import { onMounted, reactive, ref, watch } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';

  import { silentSources as apiSilentSources } from '@/api/dashboard';
  import type { SilentSource } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getPointByIds } from '@/api/point';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';

  const { t } = useI18n();
  const router = useRouter();

  // silentKey is the threshold (minutes) — "no sample within the last N min
  // for a baseline-active point" becomes flagged.
  const silentOptions = [
    { label: '15m', value: '15' },
    { label: '1h', value: '60' },
    { label: '6h', value: '360' },
  ];
  const silentKey = ref<string>('15');

  const loading = ref(false);
  const rows = ref<SilentSource[]>([]);
  const nameMap = reactive<{ devices: Record<string, string>; points: Record<string, string> }>({
    devices: {},
    points: {},
  });

  const load = async () => {
    loading.value = true;
    try {
      const res: { data?: SilentSource[] } = await apiSilentSources(7, Number(silentKey.value), 100);
      rows.value = res?.data ?? [];
      await resolveNames(rows.value);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  watch(silentKey, load);
  onMounted(load);

  const resolveNames = async (batch: SilentSource[]) => {
    const devIds = Array.from(new Set(batch.map((r) => String(r.deviceId)))).filter((id) => !nameMap.devices[id]);
    const ptIds = Array.from(new Set(batch.map((r) => String(r.pointId)))).filter((id) => !nameMap.points[id]);
    const jobs: Promise<void>[] = [];
    if (devIds.length) {
      jobs.push(
        getDeviceByIds(devIds)
          .then((r: { data?: Record<string, { deviceName?: string }> }) => {
            const d = r?.data || {};
            for (const id of devIds) if (d[id]) nameMap.devices[id] = d[id].deviceName || id;
          })
          .catch(() => {})
      );
    }
    if (ptIds.length) {
      jobs.push(
        getPointByIds(ptIds)
          .then((r: { data?: Record<string, { pointName?: string }> }) => {
            const d = r?.data || {};
            for (const id of ptIds) if (d[id]) nameMap.points[id] = d[id].pointName || id;
          })
          .catch(() => {})
      );
    }
    await Promise.all(jobs);
  };

  const deviceName = (r: SilentSource) => nameMap.devices[String(r.deviceId)] || String(r.deviceId);
  const pointName = (r: SilentSource) => nameMap.points[String(r.pointId)] || String(r.pointId);

  const formatTime = (v?: string) => {
    if (!v) return '';
    const d = new Date(v);
    if (Number.isNaN(d.getTime())) return v;
    return d.toLocaleString('zh-CN', { hour12: false });
  };

  const humanDuration = (seconds: number) => {
    if (seconds < 60) return `${seconds}s`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h`;
    return `${Math.floor(seconds / 86400)}d`;
  };

  const onRowClick = (row: SilentSource) => {
    router
      .push({ name: 'pointValue', query: { pointId: String(row.pointId), deviceId: String(row.deviceId) } })
      .catch(() => {});
  };
</script>

<style lang="scss" scoped>
  .silent-sources {
    :deep(.el-table__row) {
      cursor: pointer;
    }
  }
</style>
