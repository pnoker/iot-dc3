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

<!-- Point dashboard (位号看板): read-only data board of one point, aligned
     with the device detail page's design language — identity banner (tone
     tile + name + device link + rw chip + footer), StatCard strip, trend
     band with the shared window selector, collection health trio, value
     profile, alarm profile and the peer snapshot. All data flows in through
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

    <!-- Stat strip — mirrors the device page's __stats grid: 4 across on
         desktop, 2 on tablet, 1 on mobile. -->
    <div class="point-dashboard__stats">
      <stat-card
        :icon="OdometerIcon"
        :loading="dashboard.loading.value"
        :subtitle="unit || '—'"
        :title="$t('pointValue.dashboard.kpi.current')"
        :value="currentValueCard"
        tone="blue"
      />
      <stat-card
        :icon="TimerIcon"
        :loading="dashboard.loading.value"
        :subtitle="$t('pointValue.dashboard.kpi.medianIntervalSub')"
        :title="$t('pointValue.dashboard.kpi.medianInterval')"
        :value="stats ? formatMs(stats.medianIntervalMs) : '—'"
        tone="green"
      />
      <stat-card
        :icon="CollectionIcon"
        :loading="dashboard.loading.value"
        :subtitle="samplesSubtitle"
        :title="$t('pointValue.dashboard.kpi.samples')"
        :value="stats ? formatSampleCount(stats.sampleCount, stats.truncated) : '--'"
        tone="purple"
      />
      <stat-card
        :icon="DataLineIcon"
        :loading="dashboard.loading.value"
        :subtitle="$t('pointValue.dashboard.kpi.windowRangeSub')"
        :title="$t('pointValue.dashboard.kpi.windowRange')"
        :value="stats ? `${formatValue(stats.min)} ~ ${formatValue(stats.max)}` : '--'"
        tone="orange"
      />
      <stat-card
        :icon="WarningIcon"
        :loading="dashboard.loading.value"
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
import {
  Collection as CollectionIcon,
  Cpu,
  DataLine as DataLineIcon,
  Odometer as OdometerIcon,
  Timer as TimerIcon,
  Warning as WarningIcon,
} from '@element-plus/icons-vue';

import StatCard from '@/components/card/stat/StatCard.vue';
import router from '@/config/router';
import {copy} from '@/utils/commonUtil';
import {timestampLabel} from '@/utils/dateUtil';
import {rwFlagKey} from '@/utils/pointFormatUtil';
import {formatMs} from '@/utils/timeUtil';
import AlertProfileCard from './AlertProfileCard.vue';
import CollectionHealthCard from './CollectionHealthCard.vue';
import PeerSnapshotCard from './PeerSnapshotCard.vue';
import TrendBandCard from './TrendBandCard.vue';
import ValueProfileCard from './ValueProfileCard.vue';
import {usePointDashboard} from './usePointDashboard';
import {formatSampleCount, formatValue} from './util';

const props = defineProps({
  /** Device id of the point being boarded. */
  deviceId: {
    type: String,
    default: '',
  },
  /** Device name of the point being boarded, resolved by the detail page. */
  deviceName: {
    type: String,
    default: '',
  },
  /** Point id of the point being boarded. */
  pointId: {
    type: String,
    default: '',
  },
  /** Point name of the point being boarded, resolved by the detail page. */
  pointName: {
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
const unit = computed(() => props.unit);

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

// ---- banner ------------------------------------------------------------

// Read/write chip: the same tone language as the device status chip —
// read-write = success, read-only = warning, write-only = default.
const rwFlag = computed(() => String(props.latest?.rwFlag || '').toUpperCase());
const isReadOnly = computed(() => ['R', 'READ_ONLY'].includes(rwFlag.value));
const isReadWrite = computed(() => ['RW', 'READ_WRITE'].includes(rwFlag.value));
const rwChipClass = computed(() => {
  if (isReadWrite.value) return 'is-rw';
  if (isReadOnly.value) return 'is-ro';
  return 'is-wo';
});
const rwLabel = computed(() => t(rwFlagKey(String(props.latest?.rwFlag || ''))));

// Device attachment routes to the device detail page — same contract as
// jumpUtil's device jump ({name: 'deviceDetail', query: {id}}).
const openDevice = () => {
  void router.push({name: 'deviceDetail', query: {id: deviceId.value}}).catch(() => {
    // Navigation duplicated — the board stays where it is.
  });
};

const copyPointId = () => copy(pointId.value, t('pointValue.dashboard.banner.copyId'));

const collectTimeLabel = computed(() => timestampLabel(props.latest?.createTime));

// ---- stat strip labels -------------------------------------------------

// The latest value may be a numeric string ("42.5") or a non-numeric payload
// of string-typed points — numeric ones format with precision, others pass
// through verbatim.
const latestValueLabel = computed(() => {
  const raw = props.latest?.calValue;
  if (raw == null || raw === '') return t('pointValue.dashboard.kpi.noValue');
  const num = Number(raw);
  return String(raw).trim() !== '' && Number.isFinite(num) ? formatValue(num) : String(raw);
});

const currentValueCard = computed(() => {
  const label = latestValueLabel.value;
  if (label === t('pointValue.dashboard.kpi.noValue') || !unit.value) return label;
  return `${label} ${unit.value}`;
});

const samplesSubtitle = computed(() => {
  if (stats.value?.truncated) return t('pointValue.dashboard.kpi.samplesTruncated');
  return rangeHoursLabel.value;
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

  // Stat-card strip: 4 across on desktop, 2 on tablet, 1 on mobile.
  &__stats {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
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
