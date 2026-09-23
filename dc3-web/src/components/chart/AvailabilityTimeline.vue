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
  <div class="availability-timeline">
    <div class="availability-timeline__header">
      <span class="availability-timeline__figure">{{ coveragePct }}%</span>
      <span class="availability-timeline__figure-label">{{ t('device.detail.availabilityCoverage') }}</span>
      <span class="availability-timeline__counts">
        <span class="availability-timeline__count">{{ totalPoints }} {{ t('device.detail.availabilityDeclared') }}</span>
        <span class="availability-timeline__count availability-timeline__count--missing">
          {{ missingPoints }} {{ t('device.detail.availabilityMissing') }}
        </span>
      </span>
    </div>

    <!-- Coverage bar: present (green) vs missing (red). The backend's
         deviceCoverageGap endpoint returns a declared-but-never-seen LIST,
         not a time series, so the "life line" is reduced to this declared
         vs missing ratio rather than a fabricated timeline. -->
    <div
      v-if="totalPoints > 0"
      class="availability-timeline__bar"
      role="img"
      :aria-label="coveragePct + '% ' + t('device.detail.availabilityCoverage')"
    >
      <div
        class="availability-timeline__segment availability-timeline__segment--present"
        :style="{flexBasis: presentWidth}"
      ></div>
      <div
        class="availability-timeline__segment availability-timeline__segment--missing"
        :style="{flexBasis: missingWidth}"
      ></div>
    </div>
    <div v-else class="availability-timeline__bar availability-timeline__bar--empty"></div>

    <div class="availability-timeline__legend">
      <span class="availability-timeline__legend-item">
        <span class="availability-timeline__dot availability-timeline__dot--present"></span>
        {{ t('device.detail.availabilityPresent') }}
      </span>
      <span class="availability-timeline__legend-item">
        <span class="availability-timeline__dot availability-timeline__dot--missing"></span>
        {{ t('device.detail.availabilityMissing') }}
      </span>
    </div>

    <div v-if="missingItems.length" class="availability-timeline__section">
      <div class="availability-timeline__section-title">{{ t('device.detail.availabilityNeverSeen') }} ({{ missingItems.length }})</div>
      <ul class="availability-timeline__list">
        <li v-for="item in missingItems" :key="item.pointId" class="availability-timeline__list-item">
          <span class="availability-timeline__point">{{ item.pointId }}</span>
          <span class="availability-timeline__meta">{{ item.profileId || '-' }}</span>
        </li>
      </ul>
    </div>

    <div v-if="silentSources.length" class="availability-timeline__section">
      <div class="availability-timeline__section-title">{{ t('device.detail.availabilitySilent') }} ({{ silentSources.length }})</div>
      <ul class="availability-timeline__list">
        <li v-for="source in silentSources" :key="source.pointId" class="availability-timeline__list-item">
          <span class="availability-timeline__point">{{ source.pointId }}</span>
          <span class="availability-timeline__meta">{{ formatDuration(source.silentSeconds) }}</span>
          <span class="availability-timeline__last-seen">{{ timestamp(source.lastSeen) }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed} from 'vue';
import type {PropType} from 'vue';
import {useI18n} from 'vue-i18n';

import type {CoverageGapItem, DeviceSilentSource} from '@/config/types/dashboard';
import {timestamp} from '@/utils/dateUtil';

/**
 * The device's availability "life line", rendered against the data that
 * actually exists. `deviceCoverageGap` is a declared-but-never-seen point
 * LIST (not a time series), so this component draws a declared-vs-missing
 * coverage bar plus optional per-point silent sources, instead of inventing
 * a gap timeline. Pure presentational: the parent fetches the data.
 */
const {t} = useI18n();

const props = defineProps({
  /** Total declared points (deviceCoverageGap.totalPoints). */
  totalPoints: {
    type: Number,
    default: 0,
  },
  /** Declared-but-never-seen points (deviceCoverageGap.missingPoints). */
  missingPoints: {
    type: Number,
    default: 0,
  },
  /** The never-seen point list (deviceCoverageGap.items). */
  missingItems: {
    type: Array as PropType<CoverageGapItem[]>,
    default: () => [],
  },
  /** Per-point silence (deviceSilentSources). */
  silentSources: {
    type: Array as PropType<DeviceSilentSource[]>,
    default: () => [],
  },
});

const covered = computed(() => Math.max(0, props.totalPoints - props.missingPoints));

const coveragePct = computed(() => {
  if (props.totalPoints <= 0) return 0;
  return Math.round((covered.value / props.totalPoints) * 100);
});

const presentWidth = computed(() => {
  if (props.totalPoints <= 0) return '0%';
  return `${(covered.value / props.totalPoints) * 100}%`;
});

const missingWidth = computed(() => {
  if (props.totalPoints <= 0) return '0%';
  const missing = Math.min(props.missingPoints, props.totalPoints);
  return `${(missing / props.totalPoints) * 100}%`;
});

const formatDuration = (seconds: number): string => {
  if (seconds == null || !Number.isFinite(seconds) || seconds < 0) return '-';
  const s = Math.round(seconds);
  if (s < 60) return `${s}s`;
  const m = Math.floor(s / 60);
  if (m < 60) return `${m}m`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ${m % 60}m`;
  const d = Math.floor(h / 24);
  return `${d}d ${h % 24}h`;
};
</script>

<style lang="scss" scoped>
.availability-timeline {
  display: flex;
  flex-direction: column;
  gap: var(--dc3-space-3);
}

.availability-timeline__header {
  display: flex;
  align-items: baseline;
  gap: var(--dc3-space-2);
  flex-wrap: wrap;
}

.availability-timeline__figure {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1;
}

.availability-timeline__figure-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.availability-timeline__counts {
  margin-left: auto;
  display: flex;
  gap: var(--dc3-space-3);
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.availability-timeline__count--missing {
  color: var(--el-color-danger);
}

.availability-timeline__bar {
  display: flex;
  gap: var(--dc3-space-1);
  height: 10px;
  border-radius: var(--dc3-radius-full);
  overflow: hidden;
}

.availability-timeline__bar--empty {
  background: var(--el-fill-color);
}

.availability-timeline__segment--present {
  background: var(--el-color-success);
}

.availability-timeline__segment--missing {
  background: var(--el-color-danger);
}

.availability-timeline__legend {
  display: flex;
  gap: var(--dc3-space-4);
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.availability-timeline__legend-item {
  display: inline-flex;
  align-items: center;
  gap: var(--dc3-space-1);
}

.availability-timeline__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.availability-timeline__dot--present {
  background: var(--el-color-success);
}

.availability-timeline__dot--missing {
  background: var(--el-color-danger);
}

.availability-timeline__section {
  display: flex;
  flex-direction: column;
  gap: var(--dc3-space-2);
}

.availability-timeline__section-title {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.availability-timeline__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--dc3-space-1);
  max-height: 160px;
  overflow-y: auto;
}

.availability-timeline__list-item {
  display: flex;
  align-items: center;
  gap: var(--dc3-space-2);
  font-size: 12px;
  padding: var(--dc3-space-1) var(--dc3-space-2);
  border-radius: var(--dc3-radius-sm);
  background: var(--el-fill-color-light);
}

.availability-timeline__point {
  font-family: var(--el-font-family-mono, monospace);
  color: var(--el-text-color-primary);
}

.availability-timeline__meta {
  color: var(--el-text-color-secondary);
}

.availability-timeline__last-seen {
  margin-left: auto;
  color: var(--el-text-color-secondary);
}
</style>
