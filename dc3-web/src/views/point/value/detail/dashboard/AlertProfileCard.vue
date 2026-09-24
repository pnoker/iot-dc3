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

<!-- Alarm profile of the point (告警画像): type-distribution donut, the five
     most recent point-scoped alerts and a daily-count mini bar strip. All
     three sections share the "zero alarms" positive empty copy. -->

<template>
  <div class="alert-profile">
    <dashboard-card
      :empty="status === 'success' && typeRows.length === 0"
      :empty-text="$t('pointValue.dashboard.alert.typeEmpty')"
      :error="status === 'error'"
      :error-text="$t('common.loadFailed')"
      :footer-meta="$t('pointValue.dashboard.alert.footer')"
      :height="260"
      :loading="loading"
      :retry-text="$t('common.retry')"
      :title="$t('pointValue.dashboard.alert.typeTitle')"
      body-mode="chart"
      @refresh="emit('refresh')"
    >
      <div ref="ringChartRef" class="alert-profile__canvas"></div>
    </dashboard-card>

    <dashboard-card
      :empty="status === 'success' && dailyRows.length === 0"
      :empty-text="$t('pointValue.dashboard.alert.dailyEmpty')"
      :error="status === 'error'"
      :error-text="$t('common.loadFailed')"
      :height="200"
      :loading="loading"
      :retry-text="$t('common.retry')"
      :title="$t('pointValue.dashboard.alert.dailyTitle')"
      body-mode="chart"
      @refresh="emit('refresh')"
    >
      <div ref="dailyChartRef" class="alert-profile__canvas"></div>
    </dashboard-card>

    <dashboard-card
      :badge="recentRows.length || null"
      :empty="listStatus === 'success' && recentRows.length === 0"
      :empty-text="$t('pointValue.dashboard.alert.recentEmpty')"
      :error="listStatus === 'error'"
      :error-text="$t('common.loadFailed')"
      :height="300"
      :loading="listLoading"
      :retry-text="$t('common.retry')"
      :title="$t('pointValue.dashboard.alert.recentTitle')"
      body-mode="scroll"
      @refresh="emit('refresh')"
    >
      <el-timeline class="alert-profile__timeline">
        <el-timeline-item
          v-for="row in recentRows"
          :key="row.id"
          :hollow="row.confirmFlag === 'CONFIRMED'"
          :timestamp="formatClock(row.createTime)"
          :type="timelineColour(row.alarmTypeFlag)"
          placement="top"
        >
          <div class="alert-profile__alert-body">
            <div class="alert-profile__alert-tags">
              <el-tag :type="tagType(row.alarmTypeFlag)" size="small">
                {{ levelLabel(row.alarmTypeFlag) }}
              </el-tag>
              <el-tag v-if="row.confirmFlag === 'CONFIRMED'" effect="plain" size="small" type="success">
                {{ $t('common.confirmed') }}
              </el-tag>
              <el-tag v-else effect="plain" size="small" type="info">
                {{ $t('common.unconfirmed') }}
              </el-tag>
            </div>
            <div v-if="row.message" :title="row.message" class="alert-profile__alert-message">{{ row.message }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </dashboard-card>
  </div>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {Chart} from '@antv/g2';

import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import type {AlertEventRow, PointAlertDaily, PointAlertProfile} from '@/config/types/dashboard';
import {formatClock} from '@/utils/timeUtil';
import {chartPalette} from './util';

const props = defineProps({
  /** Alarm profile of the point (type distribution + daily trend). */
  profile: {
    type: Object as PropType<PointAlertProfile | null>,
    default: null,
  },
  /** Recent point-scoped alerts, newest first. */
  recentRows: {
    type: Array as PropType<AlertEventRow[]>,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  status: {
    type: String as PropType<'idle' | 'loading' | 'success' | 'error'>,
    default: 'idle',
  },
  /** Loader state of the recent-alert list (separate request). */
  listLoading: {
    type: Boolean,
    default: false,
  },
  listStatus: {
    type: String as PropType<'idle' | 'loading' | 'success' | 'error'>,
    default: 'idle',
  },
});

const emit = defineEmits<{ (e: 'refresh'): void }>();

const {t, locale} = useI18n();
const ringChartRef = ref<HTMLElement>();
const dailyChartRef = ref<HTMLElement>();
let ringChart: Chart | undefined;
let dailyChart: Chart | undefined;

// Human-readable label per event-ext.type tag produced by the backend,
// reusing the alarm overview's type dictionary. Unknown tags fall back to
// the raw value — better "driver-xyz" than a blank legend.
const labelFor = (type: string) => {
  const key = `settings.event.overview.types.${type.replace(/-/g, '_')}`;
  const translated = t(key);
  return translated && translated !== key ? translated : type;
};

const typeRows = computed(() => {
  const rows = props.profile?.typeDistribution ?? [];
  return rows.map((row) => ({
    type: labelFor(String(row.type ?? '-')),
    count: Number(row.count) || 0,
  }));
});

const dailyRows = computed(() => {
  const rows = props.profile?.dailyTrend ?? [];
  return [...rows]
    .sort((a: PointAlertDaily, b: PointAlertDaily) => String(a.date).localeCompare(String(b.date)))
    .map((row) => ({
      // "2026-09-22" → "09/22" — month/day is all the strip can afford.
      label: String(row.date ?? '')
        .split('-')
        .slice(1)
        .join('/'),
      date: String(row.date ?? ''),
      count: Number(row.count) || 0,
    }));
});

const destroyCharts = () => {
  ringChart?.destroy();
  ringChart = undefined;
  dailyChart?.destroy();
  dailyChart = undefined;
};

// Type distribution donut: stacked theta intervals with a right-side legend.
const drawRing = () => {
  const el = ringChartRef.value;
  if (!el) return;
  ringChart?.destroy();
  ringChart = new Chart({container: el, autoFit: true});
  ringChart
    .interval()
    .data(typeRows.value)
    .transform({type: 'stackY'})
    .coordinate({type: 'theta', innerRadius: 0.6})
    .encode('y', 'count')
    .encode('color', 'type')
    .legend('color', {position: 'right'})
    .tooltip({title: (d: {type: string}) => d.type, items: [{field: 'count'}]});
  ringChart.render();
};

// Daily alarm counts: a quiet mini bar strip, one bar per day.
const drawDaily = () => {
  const el = dailyChartRef.value;
  if (!el) return;
  dailyChart?.destroy();
  dailyChart = new Chart({container: el, autoFit: true});
  const {primary} = chartPalette();
  dailyChart
    .interval()
    .data(dailyRows.value)
    .encode('x', 'label')
    .encode('y', 'count')
    .style('fill', primary)
    .scale('y', {zero: true, nice: true})
    .axis({x: {title: false, labelAutoHide: true}, y: {title: false}})
    .legend(false);
  dailyChart.render();
};

watch(
  () => [props.profile, locale.value],
  async () => {
    await nextTick();
    if (ringChartRef.value && typeRows.value.length) drawRing();
    else {
      ringChart?.destroy();
      ringChart = undefined;
    }
    if (dailyChartRef.value && dailyRows.value.length) drawDaily();
    else {
      dailyChart?.destroy();
      dailyChart = undefined;
    }
  },
  {deep: true, flush: 'post'}
);

onMounted(async () => {
  await nextTick();
  if (ringChartRef.value && typeRows.value.length) drawRing();
  if (dailyChartRef.value && dailyRows.value.length) drawDaily();
});

onUnmounted(destroyCharts);

// ---- recent-alert row presentation (mirrors AlertList) ------------------

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
</script>

<style lang="scss" scoped>
.alert-profile {
  display: flex;
  flex-direction: column;
  gap: var(--dc3-gutter);
}

.alert-profile__canvas {
  width: 100%;
  height: 100%;
}

.alert-profile__timeline {
  padding: var(--dc3-space-2) var(--dc3-space-4) 0;

  :deep(.el-timeline-item__timestamp) {
    font-size: 12px;
    color: var(--dc3-text-muted);
    margin-bottom: 2px;
  }
}

.alert-profile__alert-body {
  display: flex;
  flex-direction: column;
  gap: var(--dc3-space-1);
}

.alert-profile__alert-tags {
  display: flex;
  align-items: center;
  gap: var(--dc3-space-2);
  flex-wrap: wrap;
}

.alert-profile__alert-message {
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
  font-size: 12px;
  color: var(--dc3-text-muted);
}
</style>
