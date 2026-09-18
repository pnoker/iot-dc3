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
  <dashboard-card
    :empty="status === 'success' && rows.length === 0"
    :empty-text="$t('settings.event.overview.topSourcesEmpty')"
    :error="status === 'error'"
    :error-text="$t('common.loadFailed')"
    :loading="loading"
    :retry-text="$t('common.retry')"
    :title="$t('settings.event.overview.topSourcesTitle')"
    body-mode="chart"
    @refresh="load"
  >
    <div ref="chartRef" class="top-sources__chart"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {Chart} from '@antv/g2';

import {alertTopSources} from '@/api/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import {useEntityNames} from '@/composables/useEntityNames';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';

const props = defineProps<{ days?: number; limit?: number }>();
const {locale} = useI18n();

const {loading, run, status} = useAsyncLoader();
const rows = ref<{name: string; count: number}[]>([]);
const chartRef = ref<HTMLElement>();
let chart: Chart | undefined;
const {resolveBySource, nameBySource} = useEntityNames();

const render = (data: { name: string; count: number }[]) => {
  const el = chartRef.value;
  if (!el) return;
  chart?.destroy();
  chart = new Chart({container: el, autoFit: true});
  chart
    .interval()
    .data(data)
    .encode('x', 'name')
    .encode('y', 'count')
    .encode('color', 'name')
    .scale('x', {padding: 0.5})
    .axis({
      x: {title: false, labelAutoRotate: false},
      y: {title: false},
    })
    .legend(false)
    .tooltip({channel: 'y', valueFormatter: (d: number) => d.toLocaleString()})
    .coordinate({transform: [{type: 'transpose'}]});
  chart.render();
};

const load = async () => {
  const days = props.days ?? 30;
  const limit = props.limit ?? 10;
  await run(
    async () => {
      const result: any = await alertTopSources(days, limit);
      const sourceRows: any[] = Array.isArray(result) ? result : [];
      await resolveBySource(sourceRows);
      return sourceRows.map((row) => ({name: nameBySource(row.source, row.sourceId), count: Number(row.count) || 0}));
    },
    {apply: (result) => (rows.value = result)}
  );
  if (status.value !== 'success') return;
  await nextTick();
  if (status.value !== 'success') return;
  if (rows.value.length > 0) render(rows.value);
  else {
    chart?.destroy();
    chart = undefined;
  }
};

onMounted(load);
watch(() => props.days, load);
watch(locale, load);
onUnmounted(() => {
  chart?.destroy();
  chart = undefined;
});

defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
.top-sources__chart {
  width: 100%;
  height: 100%;
}
</style>
