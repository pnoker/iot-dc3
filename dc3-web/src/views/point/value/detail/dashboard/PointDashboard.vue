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
    <!-- Stat strip — 6 tiles on a 3-wide grid. The first tile doubles as
         the identity card: point name as title, device·unit·rw as subtitle. -->
    <div class="point-dashboard__stats">
      <stat-card
        :icon="Cpu"
        :loading="cardLoading('current')"
        :on-refresh="() => reloadDashboard('current')"
        :subtitle="identitySubtitle"
        :title="pointName || $t('pointValue.dashboard.kpi.current')"
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
import {
  Collection as CollectionIcon,
  Cpu,
  DataLine as DataLineIcon,
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
const rangeHours = ref(24);

const {dashboard, alertProfile, recentAlerts, loadDashboard, loadAlertProfile, loadRecentAlerts} =
  usePointDashboard();

const deviceId = computed(() => String(props.deviceId || ''));
const pointId = computed(() => String(props.pointId || ''));
const unit = computed(() => props.unit);

const deviceName = computed(() => String(props.deviceName || ''));
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

// Window switch only re-fetches the trend band (the card that hosts the
// selector); stat tiles keep their values and never flash loading. Device
// or point changes re-fetch the full board with all sections.
watch(
  () => [deviceId.value, pointId.value],
  () => reloadDashboard(null),
  {immediate: true}
);
watch(
  () => rangeHours.value,
  () => {
    if (deviceId.value && pointId.value) reloadDashboard('trend');
  }
);
watch(
  () => [deviceId.value, pointId.value],
  () => {
    if (pointId.value) reloadAlertSections();
  },
  {immediate: true}
);

const rwFlag = computed(() => String(props.latest?.rwFlag || '').toUpperCase());
const rwLabel = computed(() => t(rwFlagKey(rwFlag.value)));

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
  stats.value?.truncated ? t('pointValue.dashboard.kpi.samplesTruncated') : ''
);

// Identity subtitle for the first stat tile: device · unit · rw, replacing
// the removed standalone banner.
const identitySubtitle = computed(() => {
  const parts: string[] = [];
  if (deviceName.value || deviceId.value) parts.push(deviceName.value || deviceId.value);
  if (unit.value) parts.push(unit.value);
  if (rwLabel.value) parts.push(rwLabel.value);
  return parts.join(' · ') || '—';
});</script>
<style lang="scss" scoped>
.point-dashboard {
  // Identity banner — the point-card header anatomy (tone tile + name +
  // attach) scaled up, with the rw flag as a soft chip and the point id +
  // collect time demoted to a quiet footer.
  &__banner {
    padding: var(--dc3-space-4);
    border: 1px solid var(--dc3-border-base);
    border-radius: var(--dc3-radius-lg);
    background: var(--dc3-bg-elevated);
    margin-bottom: var(--dc3-gutter);
  }

  &__hero {
    display: flex;
    align-items: center;
    gap: var(--dc3-space-3);
  }

  &__tile {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 48px;
    height: 48px;
    border: 1px solid color-mix(in srgb, var(--el-color-success) 20%, transparent);
    border-radius: var(--dc3-radius-lg);
    background: var(--el-color-success-light-9);
    color: var(--el-color-success);
  }

  &__title {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__name {
    overflow: hidden;
    font-size: 16px;
    font-weight: 650;
    color: var(--dc3-text-primary);
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__attach {
    overflow: hidden;
    font-size: 12px;
    color: var(--dc3-text-muted);
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  // Device name doubles as a quiet link to the device detail page.
  &__attach-link {
    padding: 0;
    border: 0;
    background: transparent;
    color: var(--dc3-text-secondary);
    font-size: inherit;
    cursor: pointer;

    &:hover {
      color: var(--dc3-text-brand);
      text-decoration: underline;
      text-underline-offset: 3px;
    }
  }

  // Soft tone chip — the same language as the device status chip.
  &__status-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
    height: 24px;
    padding: 0 10px;
    margin-left: auto;
    border: 1px solid var(--dc3-border-base);
    border-radius: var(--dc3-radius-full);
    background: var(--el-fill-color-light);
    color: var(--dc3-text-secondary);
    font-size: 12px;
    font-weight: 600;

    &.is-rw {
      border-color: color-mix(in srgb, var(--el-color-success) 30%, transparent);
      background: var(--el-color-success-light-9);
      color: var(--el-color-success);
    }

    &.is-ro {
      border-color: color-mix(in srgb, var(--el-color-warning) 30%, transparent);
      background: var(--el-color-warning-light-9);
      color: var(--el-color-warning);
    }

    &.is-wo {
      border-color: var(--dc3-border-base);
      background: var(--el-fill-color-light);
      color: var(--dc3-text-secondary);
    }
  }

  &__status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: currentColor;
    flex-shrink: 0;
  }

  &__footer {
    display: flex;
    align-items: center;
    gap: var(--dc3-space-3);
    margin-top: var(--dc3-space-3);
    padding-top: var(--dc3-space-3);
    border-top: 1px solid var(--dc3-border-base);
  }

  // Point id: copyable, monospace, ellipsized — a UUID is for copying,
  // not reading.
  &__code {
    overflow: hidden;
    max-width: 100%;
    padding: 0;
    border: 0;
    background: transparent;
    color: var(--dc3-text-muted);
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
    font-size: 12px;
    cursor: pointer;
    text-overflow: ellipsis;
    white-space: nowrap;

    &:hover {
      color: var(--dc3-text-brand);
      text-decoration: underline;
      text-underline-offset: 3px;
    }
  }

  &__live-meta {
    display: inline-flex;
    align-items: center;
    gap: var(--dc3-space-2);
    flex-shrink: 0;
    margin-left: auto;
    font-size: 12px;
    color: var(--dc3-text-secondary);
  }

  // Stat-card strip: 3 across on desktop, 2 on tablet, 1 on mobile —
  // six tiles close every tier with no half-empty row.
  &__stats {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: var(--dc3-space-2);
    margin-bottom: var(--dc3-gutter);

    @media (max-width: $breakpoint-md-max) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    @media (max-width: $breakpoint-xs-max) {
      grid-template-columns: 1fr;
    }
  }

  &__row {
    row-gap: var(--dc3-gutter);
    margin-bottom: var(--dc3-gutter);

    &--last {
      margin-bottom: 0;
    }
  }
}
</style>
