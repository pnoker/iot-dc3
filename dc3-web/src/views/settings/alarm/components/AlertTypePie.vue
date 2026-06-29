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
    :empty="!loading && rows.length === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.empty')"
    :loading="loading"
    :title="t('settings.event.overview.typeDistributionTitle')"
    body-mode="chart"
    @refresh="load"
  >
    <div ref="chartRef" class="alert-type-pie__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import {nextTick, onMounted, onUnmounted, ref} from 'vue';
  import {useI18n} from 'vue-i18n';
  import {Chart} from '@antv/g2';

  import {alertTypeDistribution} from '@/api/dashboard';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';

  const {t} = useI18n();

  interface TypeRow {
    type: string;
    count: number;
  }

  const loading = ref(false);
  const rows = ref<TypeRow[]>([]);
  const chartRef = ref<HTMLElement>();
  let chart: Chart | undefined;

  // Human-readable label per event-ext.type tag produced by the backend.
  // Falls back to the raw type when the backend introduces a new tag we
  // haven't localised yet — better "driver-xyz" than blank.
  const labelFor = (type: string) => {
    const key = `settings.event.overview.types.${type.replace(/-/g, '_')}`;
    const translated = t(key);
    return translated && translated !== key ? translated : type;
  };

  const render = (data: TypeRow[]) => {
    if (!chartRef.value) return;
    chart?.destroy();
    chart = new Chart({container: chartRef.value, autoFit: true});
    const mapped = data.map((r) => ({type: labelFor(r.type), count: Number(r.count) || 0}));
    chart
      .interval()
      .data(mapped)
      .transform({type: 'stackY'})
      .coordinate({type: 'theta', innerRadius: 0.6})
      .encode('y', 'count')
      .encode('color', 'type')
      .legend('color', {position: 'right'})
      .tooltip({title: (d: {type: string}) => d.type, items: [{field: 'count'}]});
    chart.render();
  };

  const load = async () => {
    loading.value = true;
    try {
      const res: {data?: TypeRow[]} = await alertTypeDistribution(30);
      rows.value = res?.data ?? [];
      await nextTick();
      if (rows.value.length > 0) render(rows.value);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  onMounted(load);
  onUnmounted(() => chart?.destroy());
  defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
  .alert-type-pie__canvas {
    width: 100%;
    height: 100%;
  }
</style>
