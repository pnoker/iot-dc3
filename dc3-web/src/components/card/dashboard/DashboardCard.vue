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

<!--
  DashboardCard — unified wrapper for every dashboard-family card on Home
  and the event-overview page. Bakes in the shared look (shadow:never,
  12px/16px header, 600/#303133 title, 8/16 footer with #fafafa bg) and
  the shared plumbing (refresh button + spinner, v-loading target, empty
  state, badge, autoRefresh interval picker), so individual cards stay
  focused on their body markup and business logic.

  Height is fixed 440 by default — the value the comments in TrendChart /
  LatencyChart call out as the "family baseline". Fixed-over-flexible is
  deliberate: el-row's align-items:stretch lets a tall LiveFeed content
  blob drag siblings with it whenever height:100% falls back to auto, so
  we break that circular dependency at the card level.
-->
<template>
  <el-card
    :class="[`dashboard-card--${bodyMode}`, {'dashboard-card--tabs': variant === 'tabs'}]"
    :style="rootStyle"
    class="dashboard-card"
    shadow="never"
  >
    <template #header>
      <div class="dashboard-card__header">
        <!-- Title region — default is text-only; `variant=tabs` hands the
             whole region to the caller (AnalyticsTabs uses it for el-tabs). -->
        <div class="dashboard-card__title">
          <slot name="title">
            <span class="dashboard-card__title-text">{{ title }}</span>
            <el-badge
              v-if="badge !== null && badge !== undefined && badge !== 0 && badge !== ''"
              :max="99"
              :value="badge"
              class="dashboard-card__badge"
              type="danger"
            />
            <span v-if="subtitle" class="dashboard-card__subtitle">{{ subtitle }}</span>
            <slot name="title-extra" />
          </slot>
        </div>

        <div class="dashboard-card__tools">
          <slot name="tools" />
          <el-segmented
            v-if="autoRefresh && autoRefresh.length"
            :model-value="interval"
            :options="autoRefresh"
            size="small"
            @update:model-value="onIntervalChange"
          />
          <el-button
            v-if="refreshable"
            :icon="Refresh"
            :loading="spinButton"
            circle
            size="small"
            @click="emit('refresh')"
          />
        </div>
      </div>
    </template>

    <div v-loading="loadBody" class="dashboard-card__body">
      <div v-if="empty" class="dashboard-card__empty">
        <el-empty :description="emptyText" :image-size="emptyImageSize" />
      </div>
      <div v-else class="dashboard-card__content">
        <slot />
      </div>
    </div>

    <div v-if="hasFooter" class="dashboard-card__footer">
      <slot name="footer" />
    </div>
  </el-card>
</template>

<script lang="ts" setup>
  import {computed, onUnmounted, useSlots, watch} from 'vue';
  import {Refresh} from '@element-plus/icons-vue';

  export type DashboardCardHeight = number | 'auto';
  export type DashboardCardBodyMode = 'plain' | 'chart' | 'scroll';
  export type DashboardCardLoadingTarget = 'body' | 'button' | 'none';
  export type DashboardCardVariant = 'default' | 'tabs';

  export interface AutoRefreshOption {
    label: string;
    value: number;
  }

  const props = withDefaults(
    defineProps<{
      /** Header-left title text. Ignored when `variant=tabs` and the caller fills the `title` slot. */
      title?: string;
      /** Red count chip next to the title. Falsy / 0 suppresses it. */
      badge?: number | string | null;
      /** Small grey line after the title — used for e.g. AlertStormSources' window hint. */
      subtitle?: string;
      /** `default` renders the title text; `tabs` hands the title region to el-tabs via the slot. */
      variant?: DashboardCardVariant;
      /** Card height. Number = fixed px (baseline 440). `auto` = min-height:300 + stretch. */
      height?: DashboardCardHeight;
      /**
       * Body layout preset:
       *  - `plain`  — default card body (padding from Element).
       *  - `chart`  — flex:1 canvas, no body padding, min-height:0 chain.
       *  - `scroll` — padding:0 + overflow:auto so long lists scroll inside the card.
       */
      bodyMode?: DashboardCardBodyMode;
      /** Spinner state. Wired to the refresh button and (optionally) a v-loading overlay. */
      loading?: boolean;
      /** Where `loading` renders its spinner. `body` covers the content, `button` only spins the refresh chip. */
      loadingTarget?: DashboardCardLoadingTarget;
      /** Show `<el-empty>` instead of the body slot when true. */
      empty?: boolean;
      emptyText?: string;
      emptyImageSize?: number;
      refreshable?: boolean;
      /** If provided, an `el-segmented` is rendered left of the refresh button (interval in ms; 0 = off). */
      autoRefresh?: AutoRefreshOption[];
      /** v-model target for the autoRefresh segmented (ms; 0 = off). */
      interval?: number;
    }>(),
    {
      title: '',
      badge: null,
      subtitle: '',
      variant: 'default',
      height: 440,
      bodyMode: 'plain',
      loading: false,
      loadingTarget: 'body',
      empty: false,
      emptyText: 'No data',
      emptyImageSize: 80,
      refreshable: true,
      autoRefresh: undefined,
      interval: 0,
    }
  );

  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'update:interval', value: number): void;
  }>();

  const slots = useSlots();
  const hasFooter = computed(() => !!slots.footer);

  const rootStyle = computed(() => {
    if (props.height === 'auto') {
      return {minHeight: '300px', height: '100%'};
    }
    return {height: `${props.height}px`};
  });

  const loadBody = computed(() => props.loading && props.loadingTarget === 'body');
  const spinButton = computed(() => props.loading && props.loadingTarget !== 'none');

  // ---- autoRefresh poll ---------------------------------------------------
  // The wrapper owns the timer so every card gets the same cadence semantics
  // (emit 'refresh' every `interval` ms when > 0, clear on unmount/change).
  // Cards just listen to @refresh and run their load() — same function they
  // pass to the manual button.
  let timer: ReturnType<typeof setInterval> | null = null;
  const clearPoll = () => {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  };
  const startPoll = (ms: number) => {
    clearPoll();
    if (ms > 0) {
      timer = setInterval(() => emit('refresh'), ms);
    }
  };
  const onIntervalChange = (value: string | number | boolean | undefined) => {
    const ms = Number(value) || 0;
    emit('update:interval', ms);
    startPoll(ms);
  };
  watch(
    () => props.interval,
    (ms) => startPoll(ms || 0),
    {immediate: true}
  );
  onUnmounted(clearPoll);

  // ---- exposed API --------------------------------------------------------
  defineExpose({
    refresh: () => emit('refresh'),
  });
</script>

<style lang="scss" scoped>
  .dashboard-card {
    display: flex;
    flex-direction: column;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    // Body holds both the content region and (if present) the footer. We use
    // the card body as a flex column so body + footer split the vertical
    // budget predictably.
    :deep(.el-card__body) {
      flex: 1;
      min-height: 0;
      display: flex;
      flex-direction: column;
      padding: 0;
    }

    // ---- header ----------------------------------------------------------
    .dashboard-card__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }

    .dashboard-card__title {
      display: inline-flex;
      align-items: baseline;
      gap: 8px;
      min-width: 0;
      flex: 1;
    }

    .dashboard-card__title-text {
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    .dashboard-card__subtitle {
      font-size: 12px;
      font-weight: normal;
      color: var(--el-text-color-secondary);
    }

    // Badge sits inline with the title — override the default floating
    // placement so it reads as part of the label rather than a superscript.
    .dashboard-card__badge {
      :deep(.el-badge__content) {
        transform: none;
        position: static;
      }
    }

    .dashboard-card__tools {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
    }

    // ---- body ------------------------------------------------------------
    .dashboard-card__body {
      flex: 1;
      min-height: 0;
      display: flex;
      flex-direction: column;
    }

    .dashboard-card__content {
      flex: 1;
      min-height: 0;
      display: flex;
      flex-direction: column;
    }

    .dashboard-card__empty {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 40px 0;
    }

    // ---- bodyMode variants ----------------------------------------------
    // plain — Element default body padding, no special overflow treatment.
    &--plain {
      :deep(.el-card__body) {
        padding: 16px;
      }

      .dashboard-card__body {
        // Padding was already applied at card-body level; content fills.
      }
    }

    // chart — the content slot is expected to host a G2 canvas that reads
    // its size from the container (autoFit). We give it full-bleed room so
    // the chart extends to the card edges; consumers typically pass a div
    // with `width: 100%; height: 100%;` inside.
    &--chart {
      .dashboard-card__content {
        width: 100%;
        height: 100%;
        padding: 8px 16px 16px;
      }
    }

    // scroll — long lists (timelines / rows). padding:0 so the list hugs
    // the card edges; overflow:auto so content never pushes past the card's
    // fixed height. This is what LiveFeed / AlertList / RecentUnconfirmed
    // all need.
    &--scroll {
      .dashboard-card__content {
        overflow: auto;
        padding: 0;
      }
    }

    // ---- footer ----------------------------------------------------------
    // Opt-in bar beneath the body. Matches LiveFeed's "updated at + rows"
    // look: 8/16 padding, secondary text, light top border and subtle bg.
    // Consumers lay out content with a pair of <span>s — flex space-between
    // handles the rest.
    .dashboard-card__footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      border-top: 1px solid var(--el-border-color-lighter);
      background: var(--el-fill-color-lighter);
      flex-shrink: 0;
    }

    // ---- variant: tabs ---------------------------------------------------
    // AnalyticsTabs replaces the title with el-tabs. Hide the default title
    // breather so the tab bar can sit flush in the header.
    &--tabs {
      .dashboard-card__title {
        gap: 0;
      }

      :deep(.el-tabs__nav-wrap::after) {
        display: none;
      }

      :deep(.el-tabs__header) {
        margin-bottom: 0;
      }
    }
  }
</style>
