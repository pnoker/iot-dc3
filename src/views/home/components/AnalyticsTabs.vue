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
  <el-card class="analytics-tabs" shadow="never">
    <template #header>
      <div class="analytics-tabs__header">
        <el-tabs v-model="activeTab" class="analytics-tabs__bar" @tab-change="onTabChange">
          <el-tab-pane v-for="t in tabs" :key="t.key" :label="t.label" :name="t.key" />
        </el-tabs>
        <div class="analytics-tabs__actions">
          <el-segmented
            v-if="isTopTab"
            v-model="rangeHours"
            :options="[
              { label: $t('home.ranges.h24'), value: 24 },
              { label: $t('home.ranges.d7'), value: 168 },
              { label: $t('home.ranges.d30'), value: 720 },
            ]"
            size="small"
            @change="load"
          />
          <el-button :icon="Refresh" :loading="loading" circle size="small" @click="load" />
        </div>
      </div>
    </template>

    <div v-loading="loading" class="analytics-tabs__body">
      <div v-if="!loading && empty" class="analytics-tabs__empty">
        <el-empty :description="$t('home.liveFeed.empty')" :image-size="80" />
      </div>
      <div v-show="!empty" ref="chartRef" class="analytics-tabs__chart"></div>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
  import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { Chart } from '@antv/g2';
  import { Refresh } from '@element-plus/icons-vue';

  import { deviceStats, driverStats, statsTop } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getDriverByIds } from '@/api/driver';
  import { getPointByIds } from '@/api/point';
  import { getProfileByIds } from '@/api/profile';

  type TabKey = 'deviceStatus' | 'protocol' | 'profile' | 'topDevice' | 'topPoint' | 'topDriver';

  const { t } = useI18n();

  const tabs = computed<{ key: TabKey; label: string }[]>(() => [
    { key: 'deviceStatus', label: t('home.tabs.deviceStatus') },
    { key: 'protocol', label: t('home.tabs.protocol') },
    { key: 'profile', label: t('home.tabs.profile') },
    { key: 'topDevice', label: t('home.tabs.topDevice') },
    { key: 'topPoint', label: t('home.tabs.topPoint') },
    { key: 'topDriver', label: t('home.tabs.topDriver') },
  ]);

  const activeTab = ref<TabKey>('deviceStatus');
  const rangeHours = ref(24);
  const loading = ref(false);
  const chartRef = ref<HTMLElement>();
  const empty = ref(false);

  const isTopTab = computed(
    () => activeTab.value === 'topDevice' || activeTab.value === 'topPoint' || activeTab.value === 'topDriver'
  );

  let chart: Chart | undefined;

  const onTabChange = () => load();

  const disposeChart = () => {
    chart?.destroy();
    chart = undefined;
  };

  const ensureChart = () => {
    if (!chartRef.value) return;
    if (chart) return;
    chart = new Chart({ container: chartRef.value, autoFit: true, height: 360 });
  };

  // Entity id → display name caches so top-N charts render labels, not ids.
  type NameKind = 'device' | 'point' | 'driver' | 'profile';
  const nameCache: Record<NameKind, Record<string, string>> = {
    device: {},
    point: {},
    driver: {},
    profile: {},
  };

  const resolveNames = async (kind: NameKind, ids: string[]) => {
    const cache = nameCache[kind];
    const missing = ids.filter((id) => id && !cache[id]);
    if (missing.length === 0) return;
    try {
      let res: any;
      if (kind === 'device') res = await getDeviceByIds(missing);
      else if (kind === 'point') res = await getPointByIds(missing);
      else if (kind === 'driver') res = await getDriverByIds(missing);
      else res = await getProfileByIds(missing);
      const data = res?.data || {};
      for (const id of missing) {
        const item = data[id];
        if (item) {
          cache[id] = item.deviceName || item.pointName || item.driverName || item.profileName || id;
        }
      }
    } catch {
      // handled globally
    }
  };

  // ---- renderers ----------------------------------------------------------
  const renderPie = (data: { key: string; count: number }[]) => {
    ensureChart();
    if (!chart) return;
    chart.clear();
    chart
      .interval()
      .data(data)
      .transform({ type: 'stackY' })
      .coordinate({ type: 'theta', innerRadius: 0.6 })
      .encode('y', 'count')
      .encode('color', 'key')
      .legend('color', { position: 'right' })
      .tooltip({ title: (d: any) => d.key, items: [{ field: 'count' }] })
      .label({ text: 'key', position: 'outside', style: { fontSize: 11 } });
    chart.render();
  };

  const renderBar = (data: { key: string; count: number }[]) => {
    ensureChart();
    if (!chart) return;
    chart.clear();
    chart
      .interval()
      .data(data)
      .encode('x', 'key')
      .encode('y', 'count')
      .encode('color', 'key')
      .legend(false)
      .axis('x', { title: false, labelAutoRotate: false })
      .axis('y', { title: false });
    chart.render();
  };

  const renderHorizontalBar = (data: { key: string; count: number }[]) => {
    ensureChart();
    if (!chart) return;
    chart.clear();
    chart
      .interval()
      .data(data)
      .encode('x', 'key')
      .encode('y', 'count')
      .encode('color', 'count')
      .coordinate({ transform: [{ type: 'transpose' }] })
      .legend(false)
      .axis('x', { title: false })
      .axis('y', { title: false });
    chart.render();
  };

  // ---- loaders ------------------------------------------------------------
  const loadDriverOrDevice = async () => {
    // deviceStatus / protocol / profile share the same two sources.
    const [drv, dev]: any = await Promise.all([driverStats(), deviceStats(10)]);
    const driverPayload = drv?.data || { byEnable: [], byType: [] };
    const devicePayload = dev?.data || { byEnable: [], byProfile: [], byDriver: [] };

    if (activeTab.value === 'deviceStatus') {
      const buckets = (devicePayload.byEnable || []) as { key: string; count: number }[];
      empty.value = buckets.length === 0;
      await nextTick();
      if (!empty.value) renderPie(buckets);
      return;
    }
    if (activeTab.value === 'protocol') {
      const buckets = (driverPayload.byType || []) as { key: string; count: number }[];
      empty.value = buckets.length === 0;
      await nextTick();
      if (!empty.value) renderBar(buckets);
      return;
    }
    if (activeTab.value === 'profile') {
      const raw = (devicePayload.byProfile || []) as { key: string; count: number }[];
      const ids = raw.map((b) => b.key).filter(Boolean);
      await resolveNames('profile', ids);
      const buckets = raw.map((b) => ({ key: nameCache.profile[b.key] || b.key, count: b.count }));
      empty.value = buckets.length === 0;
      await nextTick();
      if (!empty.value) renderHorizontalBar(buckets);
    }
  };

  const loadTop = async () => {
    const dimMap: Record<Exclude<TabKey, 'deviceStatus' | 'protocol' | 'profile'>, 'device' | 'point' | 'driver'> = {
      topDevice: 'device',
      topPoint: 'point',
      topDriver: 'driver',
    };
    const dim = dimMap[activeTab.value as keyof typeof dimMap];
    const res: any = await statsTop({ dimension: dim, rangeHours: rangeHours.value, limit: 10 });
    const rows: { entityId: number; count: number }[] = res?.data || [];
    const ids = rows.map((r) => String(r.entityId));
    await resolveNames(dim, ids);
    const buckets = rows.map((r) => ({
      key: nameCache[dim][String(r.entityId)] || String(r.entityId),
      count: r.count,
    }));
    empty.value = buckets.length === 0;
    await nextTick();
    if (!empty.value) renderHorizontalBar(buckets);
  };

  const load = async () => {
    loading.value = true;
    try {
      if (isTopTab.value) {
        await loadTop();
      } else {
        await loadDriverOrDevice();
      }
    } catch {
      // handled globally
      empty.value = true;
    } finally {
      loading.value = false;
    }
  };

  onMounted(() => {
    load();
  });

  onUnmounted(() => disposeChart());
</script>

<style lang="scss" scoped>
  .analytics-tabs {
    border-radius: 10px;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    :deep(.el-card__body) {
      padding: 0;
    }

    .analytics-tabs__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }

    .analytics-tabs__bar {
      flex: 1;
      min-width: 0;

      :deep(.el-tabs__nav-wrap::after) {
        display: none;
      }

      :deep(.el-tabs__header) {
        margin-bottom: 0;
      }
    }

    .analytics-tabs__actions {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
    }

    .analytics-tabs__body {
      position: relative;
      height: 360px;
      padding: 12px 16px 16px;
    }

    .analytics-tabs__chart {
      width: 100%;
      height: 100%;
    }

    .analytics-tabs__empty {
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
</style>
