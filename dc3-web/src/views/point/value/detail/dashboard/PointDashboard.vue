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

<!-- Point dashboard (位号看板): read-only data board of one point. KPI strip,
     trend band with the shared window selector, collection health trio, value
     profile, alarm profile and the peer snapshot. All data flows in through
     the usePointDashboard composable; switching the window re-fetches the
     dashboard payload and every window-scoped chart at once. -->

<template>
  <div class="point-dashboard">
    <!-- KPI strip — current value, median interval, window samples, window
         range, gap count. -->
    <div class="point-dashboard__kpi">
      <div class="point-dashboard__kpi-cell point-dashboard__kpi-cell--primary">
        <div class="point-dashboard__kpi-label">{{ $t('pointValue.dashboard.kpi.current') }}</div>
        <div class="point-dashboard__kpi-value">
          {{ latestValueLabel }}
          <span v-if="unit" class="point-dashboard__kpi-unit">{{ unit }}</span>
        </div>
      </div>
      <div class="point-dashboard__kpi-cell">
        <div class="point-dashboard__kpi-label">{{ $t('pointValue.dashboard.kpi.medianInterval') }}</div>
        <div class="point-dashboard__kpi-value">{{ stats ? formatMs(stats.medianIntervalMs) : '--' }}</div>
        <div class="point-dashboard__kpi-sub">{{ $t('pointValue.dashboard.kpi.medianIntervalSub') }}</div>
      </div>
      <div class="point-dashboard__kpi-cell">
        <div class="point-dashboard__kpi-label">{{ $t('pointValue.dashboard.kpi.samples') }}</div>
        <div class="point-dashboard__kpi-value">
          {{ stats ? formatSampleCount(stats.sampleCount, stats.truncated) : '--' }}
          <el-tooltip
            v-if="stats && stats.truncated"
            :content="$t('pointValue.dashboard.kpi.samplesTruncated')"
            placement="top"
          >
            <el-icon class="point-dashboard__kpi-warn"><WarningFilled/></el-icon>
          </el-tooltip>
        </div>
        <div class="point-dashboard__kpi-sub">{{ rangeHoursLabel }}</div>
      </div>
      <div class="point-dashboard__kpi-cell">
        <div class="point-dashboard__kpi-label">{{ $t('pointValue.dashboard.kpi.windowRange') }}</div>
        <div class="point-dashboard__kpi-value">
          {{ stats ? `${formatValue(stats.min)} ~ ${formatValue(stats.max)}` : '--' }}
          <span v-if="unit && stats" class="point-dashboard__kpi-unit">{{ unit }}</span>
        </div>
        <div class="point-dashboard__kpi-sub">{{ $t('pointValue.dashboard.kpi.windowRangeSub') }}</div>
      </div>
      <div class="point-dashboard__kpi-cell">
        <div class="point-dashboard__kpi-label">{{ $t('pointValue.dashboard.kpi.gaps') }}</div>
        <div class="point-dashboard__kpi-value" :class="{'is-danger': gapCount > 0, 'is-success': gapCount === 0}">
          {{ gapCount }}
        </div>
        <div class="point-dashboard__kpi-sub">{{ $t('pointValue.dashboard.kpi.gapsSub') }}</div>
      </div>
    </div>

    <!-- Trend band — hosts the shared 1h/6h/24h/7d window selector. -->
    <el-row :gutter="8" class="point-dashboard__row">
      <el-col :span="24">
        <trend-band-card
          :data="dashboard.data.value?.trend ?? []"
          :loading="dashboard.loading.value"
          :range-hours="rangeHours"
          :status="dashboard.status.value"
          :unit="unit"
          @refresh="reloadDashboard"
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
          :loading="dashboard.loading.value"
          :range-hours="rangeHours"
          :status="dashboard.status.value"
          @refresh="reloadDashboard"
        />
      </el-col>
    </el-row>

    <!-- Value profile (main column) + alarm profile (right column). -->
    <el-row :gutter="8" class="point-dashboard__row">
      <el-col :lg="16" :md="24" :sm="24" :xl="16" :xs="24">
        <value-profile-card
          :histogram="dashboard.data.value?.valueHistogram ?? []"
          :loading="dashboard.loading.value"
          :status="dashboard.status.value"
          :typical-day="dashboard.data.value?.typicalDay ?? []"
          :unit="unit"
          @refresh="reloadDashboard"
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

    <!-- Peer snapshot. -->
    <el-row :gutter="8" class="point-dashboard__row point-dashboard__row--last">
      <el-col :span="24">
        <peer-snapshot-card
          :device-id="deviceId"
          :items="peers.items.value"
          :loading="peers.loading.value"
          :status="peers.status.value"
          @refresh="reloadPeers"
        />
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {WarningFilled} from '@element-plus/icons-vue';

import AlertProfileCard from './AlertProfileCard.vue';
import CollectionHealthCard from './CollectionHealthCard.vue';
import PeerSnapshotCard from './PeerSnapshotCard.vue';
import TrendBandCard from './TrendBandCard.vue';
import ValueProfileCard from './ValueProfileCard.vue';
import {usePointDashboard} from './usePointDashboard';
import {formatSampleCount, formatValue} from './util';
import {formatMs} from '@/utils/timeUtil';

const props = defineProps({
  /** Device id of the point being boarded. */
  deviceId: {
    type: String,
    default: '',
  },
  /** Point id of the point being boarded. */
  pointId: {
    type: String,
    default: '',
  },
  /** Latest point value record already loaded by the detail page. */
  latest: {
    type: Object as PropType<Record<string, unknown>>,
    default: () => ({}),
  },
  /** Unit of the point, resolved by the detail page. */
  unit: {
    type: String,
    default: '',
  },
});

const {t} = useI18n();
const rangeHours = ref(24);

const {dashboard, alertProfile, recentAlerts, peers, loadDashboard, loadAlertProfile, loadRecentAlerts, loadPeers} =
  usePointDashboard();

const deviceId = computed(() => String(props.deviceId || ''));
const pointId = computed(() => String(props.pointId || ''));

const stats = computed(() => dashboard.data.value?.stats ?? null);
const gapCount = computed(() => dashboard.data.value?.gaps?.length ?? 0);

const reloadDashboard = () => {
  if (!deviceId.value || !pointId.value) return;
  void loadDashboard(deviceId.value, pointId.value, rangeHours.value);
};

const reloadAlertSections = () => {
  if (!pointId.value) return;
  void loadAlertProfile(pointId.value);
  void loadRecentAlerts(pointId.value);
};

const reloadPeers = () => {
  if (!deviceId.value || !pointId.value) return;
  void loadPeers(deviceId.value, pointId.value);
};

const onRangeChange = (hours: number) => {
  if (!Number.isFinite(hours) || hours <= 0 || hours > 168) return;
  rangeHours.value = hours;
};

// Window switch re-fetches the window-scoped payload; id changes re-fetch
// every section.
watch(() => [deviceId.value, pointId.value, rangeHours.value], reloadDashboard, {immediate: true});
watch(
  () => [deviceId.value, pointId.value],
  () => {
    reloadAlertSections();
    reloadPeers();
  },
  {immediate: true}
);

// ---- KPI labels --------------------------------------------------------

// The latest value may be a numeric string ("42.5") or a non-numeric payload
// of string-typed points — numeric ones format with precision, others pass
// through verbatim.
const latestValueLabel = computed(() => {
  const raw = props.latest?.calValue;
  if (raw == null || raw === '') return t('pointValue.dashboard.kpi.noValue');
  const num = Number(raw);
  return String(raw).trim() !== '' && Number.isFinite(num) ? formatValue(num) : String(raw);
});

const rangeHoursLabel = computed(() => {
  const map: Record<number, string> = {
    1: '1h',
    6: '6h',
    24: '24h',
    168: '7d',
  };
  return map[rangeHours.value] || `${rangeHours.value}h`;
});
</script>

<style lang="scss" scoped>
.point-dashboard {
  &__kpi {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: var(--dc3-space-2);
    margin-bottom: var(--dc3-gutter);
  }

  &__kpi-cell {
    padding: var(--dc3-space-3) var(--dc3-space-4);
    border: 1px solid var(--dc3-border-base);
    border-radius: var(--dc3-radius-lg);
    background: var(--dc3-bg-elevated);
    min-width: 0;

    &--primary {
      background: linear-gradient(135deg, var(--dc3-bg-elevated-strong), var(--dc3-bg-elevated));
      border-color: color-mix(in srgb, var(--el-color-primary) 25%, transparent);
    }
  }

  &__kpi-label {
    font-size: 12px;
    color: var(--dc3-text-muted);
  }

  &__kpi-value {
    display: flex;
    align-items: baseline;
    gap: var(--dc3-space-2);
    margin-top: var(--dc3-space-1);
    font-size: 24px;
    font-weight: 680;
    color: var(--dc3-text-primary);
    letter-spacing: -0.025em;
    line-height: 1.2;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;

    .point-dashboard__kpi-cell--primary & {
      font-size: 28px;
    }

    &.is-danger {
      color: var(--el-color-danger);
    }

    &.is-success {
      color: var(--el-color-success);
    }
  }

  &__kpi-unit {
    font-size: 13px;
    font-weight: 500;
    color: var(--dc3-text-muted);
  }

  &__kpi-warn {
    font-size: 16px;
    color: var(--el-color-warning);
    flex-shrink: 0;
  }

  &__kpi-sub {
    margin-top: var(--dc3-space-1);
    font-size: 11px;
    color: var(--dc3-text-muted);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
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
