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

<!-- Point dashboard: read-only data board of one point, aligned
     with the device detail page's design language — identity banner (tone
     tile + name + device link + rw chip + footer), StatCard strip, trend
     band with the shared window selector, collection health trio, value
     profile and alarm profile. All data flows in through
     the usePointDashboard composable; switching the window re-fetches the
     dashboard payload and every window-scoped chart at once. -->

<template>
  <div class="point-dashboard">
    <!-- Identity banner — the same tone-tile hero anatomy as the device
         detail page: tile + name + attachment + status chip, with the
         point id and collect time demoted to a quiet footer. -->
    <div class="point-dashboard__banner">
      <div class="point-dashboard__hero">
        <span class="point-dashboard__tile" aria-hidden="true">
          <el-icon :size="26"><Cpu/></el-icon>
        </span>
        <div class="point-dashboard__title">
          <span class="point-dashboard__name">{{ pointName || '-' }}</span>
          <span class="point-dashboard__attach">
            <button
              v-if="deviceId"
              class="point-dashboard__attach-link"
              :title="deviceName || deviceId"
              type="button"
              @click="openDevice"
            >
              {{ deviceName || deviceId }}
            </button>
            <template v-if="unit"> · {{ unit }}</template>
          </span>
        </div>
        <span v-if="rwFlag" class="point-dashboard__status-chip" :class="rwChipClass">
          <span class="point-dashboard__status-dot"></span>
          {{ rwLabel }}
        </span>
      </div>
      <div class="point-dashboard__footer">
        <button
          v-if="pointId"
          class="point-dashboard__code"
          :title="pointId"
          type="button"
          @click="copyPointId"
        >
          {{ pointId }}
        </button>
        <span class="point-dashboard__live-meta">
          {{ $t('pointValue.card.collectTime') }} {{ collectTimeLabel }}
        </span>
      </div>
    </div>

    <!-- Stat strip — 6 tiles on a 3-wide grid so every row stays full:
         2 rows of 3 on desktop, 3 rows of 2 on tablet, 6 stacked on
         mobile. Six (not five) is what makes the row math close at the
         3/2/1 column tiers. -->
    <div class="point-dashboard__stats">
      <stat-card
        :icon="OdometerIcon"
        :loading="cardLoading('current')"
        :on-refresh="() => reloadDashboard('current')"
        :subtitle="unit || '—'"
        :title="$t('pointValue.dashboard.kpi.current')"
        :value="currentValueCard"
        tone="blue"
      />
      <stat-card
        :icon="DataLineIcon"
        :loading="cardLoading('average')"
        :on-refresh="() => reloadDashboard('average')"
        :subtitle="$t('pointValue.dashboard.kpi.averageSub')"
        :title="$t('pointValue.dashboard.kpi.average')"
        :value="averageValueCard"
        tone="green"
      />
      <stat-card
        :icon="TrendChartsIcon"
        :loading="cardLoading('range')"
        :on-refresh="() => reloadDashboard('range')"
        :subtitle="$t('pointValue.dashboard.kpi.windowRangeSub')"
        :title="$t('pointValue.dashboard.kpi.windowRange')"
        :value="stats ? `${formatValue(stats.min)} ~ ${formatValue(stats.max)}` : '--'"
        tone="orange"
      />
      <stat-card
        :icon="TimerIcon"
        :loading="cardLoading('interval')"
        :on-refresh="() => reloadDashboard('interval')"
        :subtitle="$t('pointValue.dashboard.kpi.medianIntervalSub')"
        :title="$t('pointValue.dashboard.kpi.medianInterval')"
        :value="stats ? `${Number(stats.medianIntervalMs) / 1000}s` : '—'"
        tone="purple"
      />
      <stat-card
        :icon="CollectionIcon"
        :loading="cardLoading('samples')"
        :on-refresh="() => reloadDashboard('samples')"
        :subtitle="samplesSubtitle"
        :title="$t('pointValue.dashboard.kpi.samples')"
        :value="stats ? formatSampleCount(stats.sampleCount, stats.truncated) : '--'"
        tone="blue"
      />
      <stat-card
        :icon="WarningIcon"
        :loading="cardLoading('gaps')"
        :on-refresh="() => reloadDashboard('gaps')"
        :subtitle="$t('pointValue.dashboard.kpi.gapsSub')"
        :title="$t('pointValue.dashboard.kpi.gaps')"
        :value="gapCount"
        tone="red"
      />
    </div>

    <!-- Trend band — hosts the shared 1h/6h/24h/7d window selector. -->
    <el-row :gutter="8" class="point-dashboard__row">
      <el-col :span="24">
        <trend-band-card
          :data="dashboard.data.value?.trend ?? []"
          :loading="cardLoading('trend')"
          :range-hours="rangeHours"
          :status="dashboard.status.value"
          :unit="unit"
          @refresh="() => reloadDashboard('trend')"
          @update:range-hours="onRangeChange"
        />
      </el-col>
    </el-row>

    <!-- Collection health trio. -->
    <el-row :gutter="8" class="point-dashboard__row">
      <el-col :span="24">
        <collection-health-card
          :gaps="dashboard.data.value?.gaps ?? []"
          :hourly-volume="dashboard.data.value?.hourlyVolume ?? []"
          :interval-histogram="dashboard.data.value?.intervalHistogram ?? []"
          :loading="cardLoading('collection')"
          :range-hours="rangeHours"
          :status="dashboard.status.value"
          @refresh="() => reloadDashboard('collection')"
        />
      </el-col>
    </el-row>

    <!-- Value profile (main column) + alarm profile (right column). -->
    <el-row :gutter="8" class="point-dashboard__row">
      <el-col :lg="16" :md="24" :sm="24" :xl="16" :xs="24">
        <value-profile-card
          :histogram="dashboard.data.value?.valueHistogram ?? []"
          :loading="cardLoading('profile')"
          :status="dashboard.status.value"
          :typical-day="dashboard.data.value?.typicalDay ?? []"
          :unit="unit"
          @refresh="() => reloadDashboard('profile')"
        />
      </el-col>
      <el-col :lg="8" :md="24" :sm="24" :xl="8" :xs="24">
        <alert-profile-card
          :list-loading="recentAlerts.loading.value"
          :list-status="recentAlerts.status.value"
          :loading="alertProfile.loading.value"
          :profile="alertProfile.data.value"
          :recent-rows="recentAlerts.rows.value"
          :status="alertProfile.status.value"
          @refresh="reloadAlertSections"
        />
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';
import {
  Collection as CollectionIcon,
  Cpu,
  DataLine as DataLineIcon,
  Odometer as OdometerIcon,
  Timer as TimerIcon,
  TrendCharts as TrendChartsIcon,
  Warning as WarningIcon,
} from '@element-plus/icons-vue';

import AlertProfileCard from './AlertProfileCard.vue';
import CollectionHealthCard from './CollectionHealthCard.vue';
import TrendBandCard from './TrendBandCard.vue';
import ValueProfileCard from './ValueProfileCard.vue';
import {usePointDashboard} from './usePointDashboard';
import {formatSampleCount, formatValue} from './util';
import StatCard from '@/components/card/stat/StatCard.vue';
import {rwFlagKey} from '@/utils/pointFormatUtil';

const props = defineProps({
  deviceId: {type: String, default: ''},
  deviceName: {type: String, default: ''},
  latest: {type: Object as PropType<Record<string, any>>, default: () => ({})},
  pointId: {type: String, default: ''},
  pointName: {type: String, default: ''},
  unit: {type: String, default: ''},
});

const {t} = useI18n();
const router = useRouter();
const rangeHours = ref(24);

const {dashboard, alertProfile, recentAlerts, loadDashboard, loadAlertProfile, loadRecentAlerts} =
  usePointDashboard();

const deviceId = computed(() => String(props.deviceId || ''));
const pointId = computed(() => String(props.pointId || ''));
const unit = computed(() => props.unit);

const stats = computed(() => dashboard.data.value?.stats ?? null);
const gapCount = computed(() => dashboard.data.value?.gaps?.length ?? 0);

// --- Per-card refresh loading + selective data application -------------------
const refreshingCard = ref<string | null>(null);

const CARD_SECTIONS: Record<string, readonly string[]> = {
  current: ['stats'],
  average: ['stats'],
  range: ['stats'],
  interval: ['stats'],
  samples: ['stats'],
  gaps: ['stats', 'gaps'],
  trend: ['trend'],
  collection: ['gaps', 'hourlyVolume', 'intervalHistogram'],
  profile: ['valueHistogram', 'typicalDay'],
};

const cardLoading = (cardId: string) =>
  dashboard.loading.value && (!refreshingCard.value || refreshingCard.value === cardId);

const reloadDashboard = (cardId: string | null = null) => {
  if (!deviceId.value || !pointId.value) return;
  refreshingCard.value = cardId;
  const sections = cardId ? CARD_SECTIONS[cardId] ?? null : null;
  void loadDashboard(deviceId.value, pointId.value, rangeHours.value, sections).finally(() => {
    refreshingCard.value = null;
  });
};

const reloadAlertSections = () => {
  if (!pointId.value) return;
  void loadAlertProfile(pointId.value);
  void loadRecentAlerts(pointId.value);
};

const onRangeChange = (hours: number) => {
  if (!Number.isFinite(hours) || hours <= 0 || hours > 168) return;
  rangeHours.value = hours;
};

watch(() => [deviceId.value, pointId.value, rangeHours.value], () => reloadDashboard(null), {immediate: true});
watch(
  () => [deviceId.value, pointId.value],
  () => {
    if (pointId.value) reloadAlertSections();
  },
  {immediate: true}
);

// ---- banner helpers --------------------------------------------------------

const rwFlag = computed(() => String(props.latest?.rwFlag || '').toUpperCase());
const isReadOnly = computed(() => ['R', 'READ_ONLY'].includes(rwFlag.value));
const isWriteOnly = computed(() => ['W', 'WRITE_ONLY'].includes(rwFlag.value));
const rwChipClass = computed(() =>
  isWriteOnly.value
    ? 'point-dashboard__status-chip--default'
    : isReadOnly.value
      ? 'point-dashboard__status-chip--warning'
      : 'point-dashboard__status-chip--success'
);
const rwLabel = computed(() => t(rwFlagKey(rwFlag.value)));

const openDevice = () => {
  if (!deviceId.value) return;
  router.push({name: 'deviceDetail', query: {id: deviceId.value}}).catch(() => {
    // handled globally
  });
};

const copyPointId = async () => {
  if (!pointId.value) return;
  try {
    await navigator.clipboard.writeText(pointId.value);
  } catch {
    // clipboard unavailable — silent
  }
};

// ---- stat card values -------------------------------------------------------

const currentValueCard = computed(() => {
  const raw = props.latest?.calValue;
  if (raw === undefined || raw === null) return '--';
  return `${formatValue(Number(raw))} ${unit.value || ''}`.trim();
});

const averageValueCard = computed(() =>
  stats.value ? `${formatValue(stats.value.avg)} ${unit.value || ''}`.trim() : '--'
);

const samplesSubtitle = computed(() =>
  stats.value ? `${rangeHours.value}h window` : `${rangeHours.value}h window`
);

const collectTimeLabel = computed(() => String(props.latest?.createTime || '—'));</script>
