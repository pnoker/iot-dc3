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
  <div class="quality-ring">
    <div class="quality-ring__donut">
      <svg class="quality-ring__svg" viewBox="0 0 120 120">
        <circle class="quality-ring__track" cx="60" cy="60" :r="RADIUS" />
        <circle
          class="quality-ring__arc"
          cx="60"
          cy="60"
          :r="RADIUS"
          :style="{stroke: arcColor, strokeDasharray: arcLength + ' ' + circumference}"
          transform="rotate(-90 60 60)"
        />
        <text class="quality-ring__value" x="60" y="57" text-anchor="middle">{{ ratioLabel }}</text>
        <text class="quality-ring__sub" x="60" y="73" text-anchor="middle">{{ t('device.detail.qualityNumeric') }}</text>
      </svg>
    </div>
    <div class="quality-ring__caption">
      <span>{{ totalSamples }} {{ t('device.detail.samples') }}</span>
      <span class="quality-ring__caption-sep">·</span>
      <span>{{ nonNumericSamples }} {{ t('device.detail.nonNumeric') }}</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed} from 'vue';
import type {PropType} from 'vue';
import {useI18n} from 'vue-i18n';

import type {DeviceQuality} from '@/config/types/dashboard';

/**
 * The three numeric fields the ring actually reads. Accepts a full
 * `DeviceQuality` (the deviceQuality endpoint shape) or a bare
 * `{numericRatio, totalSamples, nonNumericSamples}` object.
 */
type QualityInput = Pick<DeviceQuality, 'numericRatio' | 'totalSamples' | 'nonNumericSamples'>;

const {t} = useI18n();

const RADIUS = 52;
const circumference = 2 * Math.PI * RADIUS;

const props = defineProps({
  /** Data-quality summary; null renders an empty ring. */
  quality: {
    type: Object as PropType<QualityInput | null>,
    default: null,
  },
});

const ratio = computed(() => {
  const value = props.quality?.numericRatio;
  if (value == null || !Number.isFinite(value)) return 0;
  return Math.min(100, Math.max(0, value));
});

const totalSamples = computed(() => props.quality?.totalSamples ?? 0);
const nonNumericSamples = computed(() => props.quality?.nonNumericSamples ?? 0);

// Arc tone by numeric ratio: >=95 healthy, >=80 warning, else critical.
const tone = computed<'success' | 'warning' | 'danger'>(() => {
  if (ratio.value >= 95) return 'success';
  if (ratio.value >= 80) return 'warning';
  return 'danger';
});

const arcColor = computed(() => `var(--el-color-${tone.value})`);
const arcLength = computed(() => (ratio.value / 100) * circumference);

const ratioLabel = computed(() => {
  if (!props.quality) return '—';
  const value = ratio.value;
  return `${Number.isInteger(value) ? value : value.toFixed(1)}%`;
});
</script>

<style lang="scss" scoped>
.quality-ring {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--dc3-space-2);
}

.quality-ring__donut {
  width: 100%;
  max-width: 160px;
}

.quality-ring__svg {
  display: block;
  width: 100%;
  height: auto;
}

.quality-ring__track {
  fill: none;
  stroke: var(--el-fill-color);
  stroke-width: 12;
}

.quality-ring__arc {
  fill: none;
  stroke-width: 12;
  stroke-linecap: round;
  transition: stroke 0.3s ease, stroke-dasharray 0.3s ease;
}

.quality-ring__value {
  font-size: 24px;
  font-weight: 600;
  fill: var(--el-text-color-primary);
}

.quality-ring__sub {
  font-size: 11px;
  fill: var(--el-text-color-secondary);
}

.quality-ring__caption {
  display: flex;
  align-items: center;
  gap: var(--dc3-space-1);
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.quality-ring__caption-sep {
  color: var(--el-border-color);
}
</style>
