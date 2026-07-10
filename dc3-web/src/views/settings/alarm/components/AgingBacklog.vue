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
    :empty="!loading && data.total === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.agingEmpty')"
    :loading="loading"
    :subtitle="t('settings.event.overview.agingSubtitle', {n: data.total})"
    :title="t('settings.event.overview.agingTitle')"
    body-mode="chart"
    class="aging-backlog"
    @refresh="load"
  >
    <div ref="chartRef" class="aging-backlog__canvas"></div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {nextTick, onMounted, onUnmounted, reactive, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {Chart} from '@antv/g2';

import {alertAging} from '@/api/dashboard';
import type {AgingBacklog} from '@/config/types/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';

const {t} = useI18n();
const {loading, run} = useAsyncLoader();

const data = reactive<AgingBacklog>({under1h: 0, h1to6: 0, h6to24: 0, over24h: 0, total: 0});
const chartRef = ref<HTMLElement>();
let chart: Chart | undefined;

// Palette is green→yellow→orange→red — progressively more alarming as
// unconfirmed alarms age. The 24h+ bucket is the SLA breach indicator.
const colours = ['#67c23a', '#e6a23c', '#f56c6c', '#c45656'];

const render = () => {
  const el = chartRef.value;
  if (!el) return;
  chart?.destroy();
  chart = new Chart({container: el, autoFit: true});
  const rows = [
    {bucket: t('settings.event.overview.agingBucketUnder1h'), count: data.under1h, idx: 0},
    {bucket: t('settings.event.overview.agingBucket1to6'), count: data.h1to6, idx: 1},
    {bucket: t('settings.event.overview.agingBucket6to24'), count: data.h6to24, idx: 2},
    {bucket: t('settings.event.overview.agingBucketOver24'), count: data.over24h, idx: 3},
  ];
  chart
    .interval()
    .data(rows)
    .encode('x', 'bucket')
    .encode('y', 'count')
    .encode('color', 'idx')
    .scale('color', {range: colours})
    .legend(false)
    .axis({x: {title: false, labelAutoRotate: false}, y: {title: false}})
    .label({text: 'count', position: 'top', style: {fontSize: 11}});
  chart.render();
};

const load = () =>
  run(async () => {
    const res: { data?: AgingBacklog } = await alertAging();
    Object.assign(data, res?.data ?? {under1h: 0, h1to6: 0, h6to24: 0, over24h: 0, total: 0});
    await nextTick();
    if (data.total > 0) render();
  });

onMounted(load);
onUnmounted(() => chart?.destroy());

defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
.aging-backlog__canvas {
  width: 100%;
  height: 100%;
}
</style>
