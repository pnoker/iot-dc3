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
  <el-card :class="`stat-card--${tone}`" class="stat-card" shadow="hover" @click="emit('click')">
    <div class="stat-card__row">
      <div class="stat-card__icon">
        <el-icon :size="28">
          <component :is="icon"/>
        </el-icon>
      </div>
      <div class="stat-card__body">
        <div class="stat-card__title">{{ title }}</div>
        <div class="stat-card__value">
          <span class="stat-card__value-text">{{ formattedValue }}</span>
          <span v-if="trend" :class="['stat-card__trend', `stat-card__trend--${trend.direction}`]">
            <el-icon><component :is="trendIcon"/></el-icon>
            {{ trend.label }}
          </span>
        </div>
        <div v-if="subtitle" class="stat-card__subtitle">{{ subtitle }}</div>
      </div>
      <el-button
        v-if="onRefresh"
        :icon="Refresh"
        :loading="refreshing"
        circle
        class="stat-card__refresh"
        size="small"
        text
        @click.stop="doRefresh"
      />
    </div>
    <!-- Sparkline area is always rendered so every StatCard lines up at
         the same height; MiniAreaChart is kept mounted even when the
         series is empty (it paints nothing until data arrives) so the
         chart's lifecycle matches PointValueCard's — lazy mount under a
         v-if would create a subtle race with the internal onMounted
         draw and empty cards in the same row. -->
    <div class="stat-card__spark">
      <mini-area-chart :color="accentColor" :data="sparkline" :height="40"/>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
import type {Component, PropType} from 'vue';
import {computed, ref} from 'vue';
import {CaretBottom, CaretTop, Minus, Refresh} from '@element-plus/icons-vue';

import MiniAreaChart from '@/components/chart/MiniAreaChart.vue';

interface Trend {
  direction: 'up' | 'down' | 'flat';
  label: string;
}

const props = defineProps({
  title: {type: String, required: true},
  value: {type: [Number, String], default: 0},
  subtitle: {type: String, default: ''},
  icon: {type: [String, Object] as PropType<string | Component>, required: true},
  tone: {type: String as PropType<'blue' | 'green' | 'orange' | 'purple' | 'red'>, default: 'blue'},
  trend: {type: Object as PropType<Trend | null>, default: null},
  sparkline: {type: Array as PropType<number[]>, default: () => []},
  /**
   * Optional refresh handler. When provided, a small text button appears
   * in the top-right of the card; clicking it invokes the handler without
   * bubbling the card-level @click (so navigation isn't triggered by a
   * refresh).
   */
  onRefresh: {type: Function as unknown as PropType<(() => Promise<void> | void) | null>, default: null},
});

const emit = defineEmits<{ (e: 'click'): void }>();

const refreshing = ref(false);
const doRefresh = async () => {
  if (!props.onRefresh) return;
  refreshing.value = true;
  try {
    await props.onRefresh();
  } finally {
    refreshing.value = false;
  }
};

const formattedValue = computed(() => {
  const v = Number(props.value);
  if (Number.isNaN(v)) return String(props.value);
  if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(1)}M`;
  if (v >= 1_000) return `${(v / 1_000).toFixed(1)}k`;
  return v.toLocaleString();
});

const trendIcon = computed(() => {
  if (!props.trend) return Minus;
  if (props.trend.direction === 'up') return CaretTop;
  if (props.trend.direction === 'down') return CaretBottom;
  return Minus;
});

// Map each tone to the accent colour so MiniAreaChart can stroke/fill
// the sparkline in the card's own palette. Kept in lock-step with the
// --stat-card-accent values declared in the scoped styles below so a
// palette change only needs updating in one place conceptually.
const TONE_ACCENT: Record<string, string> = {
  blue: 'var(--el-color-primary)',
  green: 'var(--el-color-success)',
  orange: 'var(--el-color-warning)',
  purple: 'var(--dc3-color-purple)',
  red: 'var(--el-color-danger)',
};
const accentColor = computed(() => TONE_ACCENT[props.tone] || TONE_ACCENT.blue);
</script>

<style lang="scss" scoped>
.stat-card {
  --stat-card-accent: var(--el-color-primary);
  --stat-card-bg: var(--el-color-primary-light-9);
  position: relative;
  isolation: isolate;
  overflow: hidden;
  cursor: pointer;
  transition:
    transform var(--dc3-duration-base) var(--dc3-ease-standard),
    border-color var(--dc3-duration-base) var(--dc3-ease-standard),
    box-shadow var(--dc3-duration-base) var(--dc3-ease-standard);
  height: 100%;
  min-height: 132px;
  display: flex;
  flex-direction: column;

  &::before {
    position: absolute;
    z-index: -1;
    top: -46px;
    right: -42px;
    width: 118px;
    height: 118px;
    border-radius: 50%;
    background: var(--stat-card-bg);
    content: '';
    filter: blur(2px);
  }

  &::after {
    position: absolute;
    top: 0;
    right: var(--dc3-space-4);
    left: var(--dc3-space-4);
    height: 2px;
    border-radius: 0 0 var(--dc3-radius-full) var(--dc3-radius-full);
    background: linear-gradient(90deg, transparent, var(--stat-card-accent), transparent);
    opacity: 0.7;
    content: '';
  }

  :deep(.el-card__body) {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  &:hover {
    border-color: color-mix(in srgb, var(--stat-card-accent) 30%, transparent);
    transform: translateY(-3px);
  }

  &--blue {
    --stat-card-accent: var(--el-color-primary);
    --stat-card-bg: var(--el-color-primary-light-9);
  }

  &--green {
    --stat-card-accent: var(--el-color-success);
    --stat-card-bg: var(--el-color-success-light-9);
  }

  &--orange {
    --stat-card-accent: var(--el-color-warning);
    --stat-card-bg: var(--el-color-warning-light-9);
  }

  &--purple {
    --stat-card-accent: var(--dc3-color-purple);
    --stat-card-bg: var(--dc3-color-purple-soft);
  }

  &--red {
    --stat-card-accent: var(--el-color-danger);
    --stat-card-bg: var(--el-color-danger-light-9);
  }

  :deep(.el-card__body) {
    padding: var(--dc3-space-4);
  }

  .stat-card__row {
    display: flex;
    align-items: flex-start;
    gap: var(--dc3-space-3);
    position: relative;
  }

  .stat-card__refresh {
    position: absolute;
    top: -4px;
    right: -4px;
    opacity: 0;
    transition: opacity 120ms ease;
  }

  &:hover .stat-card__refresh {
    opacity: 1;
  }

  .stat-card__icon {
    width: 44px;
    height: 44px;
    border: 1px solid color-mix(in srgb, var(--stat-card-accent) 18%, transparent);
    border-radius: var(--dc3-radius-lg);
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--stat-card-bg);
    color: var(--stat-card-accent);
    flex-shrink: 0;
  }

  .stat-card__body {
    flex: 1;
    min-width: 0;
  }

  .stat-card__title {
    font-size: 13px;
    color: var(--dc3-text-muted);
    margin-bottom: 2px;
  }

  .stat-card__value {
    display: flex;
    align-items: baseline;
    gap: 8px;
    line-height: 1.2;
  }

  .stat-card__value-text {
    font-size: 25px;
    font-weight: 680;
    color: var(--dc3-text-primary);
    letter-spacing: -0.025em;
  }

  .stat-card__trend {
    font-size: 12px;
    display: inline-flex;
    align-items: center;
    gap: 2px;
  }

  .stat-card__trend--up {
    color: var(--el-color-success);
  }

  .stat-card__trend--down {
    color: var(--el-color-danger);
  }

  .stat-card__trend--flat {
    color: var(--el-text-color-secondary);
  }

  .stat-card__subtitle {
    font-size: 12px;
    color: var(--dc3-text-muted);
    margin-top: 4px;
  }

  .stat-card__spark {
    height: 40px;
    margin-top: auto;
    padding-top: 8px;
  }
}
</style>
