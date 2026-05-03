<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <el-card class="stat-card" :class="`stat-card--${tone}`" shadow="hover" @click="emit('click')">
    <div class="stat-card__row">
      <div class="stat-card__icon">
        <el-icon :size="28">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="stat-card__body">
        <div class="stat-card__title">{{ title }}</div>
        <div class="stat-card__value">
          <span class="stat-card__value-text">{{ formattedValue }}</span>
          <span v-if="trend" :class="['stat-card__trend', `stat-card__trend--${trend.direction}`]">
            <el-icon><component :is="trendIcon" /></el-icon>
            {{ trend.label }}
          </span>
        </div>
        <div v-if="subtitle" class="stat-card__subtitle">{{ subtitle }}</div>
      </div>
      <el-button
        v-if="onRefresh"
        class="stat-card__refresh"
        :icon="Refresh"
        :loading="refreshing"
        circle
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
      <mini-area-chart :data="sparkline" :color="accentColor" :height="40" />
    </div>
  </el-card>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import type { Component, PropType } from 'vue';
  import { CaretBottom, CaretTop, Minus, Refresh } from '@element-plus/icons-vue';

  import MiniAreaChart from '@/components/chart/MiniAreaChart.vue';

  interface Trend {
    direction: 'up' | 'down' | 'flat';
    label: string;
  }

  const props = defineProps({
    title: { type: String, required: true },
    value: { type: [Number, String], default: 0 },
    subtitle: { type: String, default: '' },
    icon: { type: [String, Object] as PropType<string | Component>, required: true },
    tone: { type: String as PropType<'blue' | 'green' | 'orange' | 'purple' | 'red'>, default: 'blue' },
    trend: { type: Object as PropType<Trend | null>, default: null },
    sparkline: { type: Array as PropType<number[]>, default: () => [] },
    /**
     * Optional refresh handler. When provided, a small text button appears
     * in the top-right of the card; clicking it invokes the handler without
     * bubbling the card-level @click (so navigation isn't triggered by a
     * refresh).
     */
    onRefresh: { type: Function as unknown as PropType<(() => Promise<void> | void) | null>, default: null },
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
    blue: '#409eff',
    green: '#67c23a',
    orange: '#e6a23c',
    purple: '#9059f6',
    red: '#f56c6c',
  };
  const accentColor = computed(() => TONE_ACCENT[props.tone] || TONE_ACCENT.blue);
</script>

<style lang="scss" scoped>
  .stat-card {
    --stat-card-accent: #409eff;
    --stat-card-bg: rgba(64, 158, 255, 0.08);
    border-radius: 10px;
    cursor: pointer;
    transition: transform 0.15s ease;
    height: 100%;
    min-height: 132px;
    display: flex;
    flex-direction: column;

    :deep(.el-card__body) {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    &:hover {
      transform: translateY(-2px);
    }

    &--blue {
      --stat-card-accent: #409eff;
      --stat-card-bg: rgba(64, 158, 255, 0.1);
    }
    &--green {
      --stat-card-accent: #67c23a;
      --stat-card-bg: rgba(103, 194, 58, 0.1);
    }
    &--orange {
      --stat-card-accent: #e6a23c;
      --stat-card-bg: rgba(230, 162, 60, 0.1);
    }
    &--purple {
      --stat-card-accent: #9059f6;
      --stat-card-bg: rgba(144, 89, 246, 0.1);
    }
    &--red {
      --stat-card-accent: #f56c6c;
      --stat-card-bg: rgba(245, 108, 108, 0.1);
    }

    :deep(.el-card__body) {
      padding: 16px 18px;
    }

    .stat-card__row {
      display: flex;
      align-items: flex-start;
      gap: 14px;
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
      width: 48px;
      height: 48px;
      border-radius: 10px;
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
      color: #909399;
      margin-bottom: 2px;
    }

    .stat-card__value {
      display: flex;
      align-items: baseline;
      gap: 8px;
      line-height: 1.2;
    }

    .stat-card__value-text {
      font-size: 24px;
      font-weight: 600;
      color: #303133;
    }

    .stat-card__trend {
      font-size: 12px;
      display: inline-flex;
      align-items: center;
      gap: 2px;
    }

    .stat-card__trend--up {
      color: #67c23a;
    }
    .stat-card__trend--down {
      color: #f56c6c;
    }
    .stat-card__trend--flat {
      color: #909399;
    }

    .stat-card__subtitle {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }

    .stat-card__spark {
      height: 40px;
      margin-top: auto;
      padding-top: 8px;
    }
  }
</style>
