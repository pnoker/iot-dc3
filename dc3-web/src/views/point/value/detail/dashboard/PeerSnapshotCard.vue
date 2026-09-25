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

<!-- Peer snapshot (邻位快照): the other points of the same device with their
     latest values, as a clickable card grid that routes to each point's own
     detail page. -->

<template>
  <dashboard-card
    :empty="status === 'success' && items.length === 0"
    :empty-text="$t('pointValue.dashboard.peer.empty')"
    :error="status === 'error'"
    :error-text="$t('common.loadFailed')"
    :footer-meta="$t('pointValue.dashboard.peer.footer')"
    height="auto"
    :loading="loading"
    :retry-text="$t('common.retry')"
    :title="$t('pointValue.dashboard.peer.title')"
    body-mode="plain"
    @refresh="emit('refresh')"
  >
    <div class="peer-snapshot__grid">
      <el-card
        v-for="item in items"
        :key="item.pointId"
        :aria-label="item.pointName"
        :title="$t('pointValue.dashboard.peer.tooltip')"
        class="peer-snapshot__card"
        role="button"
        shadow="hover"
        tabindex="0"
        @click="openPeer(item.pointId)"
        @keydown.enter="openPeer(item.pointId)"
      >
        <div class="peer-snapshot__body">
          <span class="peer-snapshot__name">{{ item.pointName || item.pointId }}</span>
          <span class="peer-snapshot__value">
            <template v-if="item.value !== null">
              <span class="peer-snapshot__value-text">{{ displayValue(item.value) }}</span>
              <span v-if="item.unit" class="peer-snapshot__unit">{{ item.unit }}</span>
            </template>
            <span v-else class="peer-snapshot__no-value">{{ $t('pointValue.dashboard.peer.noValue') }}</span>
          </span>
        </div>
        <!-- Family footer form: a full-width strip under the body instead of
             another body row — the collect time is metadata, not content. -->
        <div class="peer-snapshot__foot">
          <span class="peer-snapshot__time">{{ item.createTime ? formatDateTime(item.createTime) : '' }}</span>
        </div>
      </el-card>
    </div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';

import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import router from '@/config/router';
import {formatDateTime} from '@/utils/timeUtil';
import type {PeerSnapshotItem} from './usePointDashboard';

const props = defineProps({
  /** Device owning the snapshot — needed to build the peer detail route. */
  deviceId: {
    type: String,
    default: '',
  },
  /** Neighbour point rows joined with their latest values. */
  items: {
    type: Array as PropType<PeerSnapshotItem[]>,
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
});

const emit = defineEmits<{ (e: 'refresh'): void }>();

// Latest values may be numeric strings ("42.5") or non-numeric payloads of
// string-typed points — numeric ones format with precision, others pass
// through verbatim.
const displayValue = (raw: string): string => {
  const num = Number(raw);
  if (String(raw).trim() !== '' && Number.isFinite(num)) {
    return num.toLocaleString('en-US', {maximumFractionDigits: 4});
  }
  return raw;
};

// Route to the peer point's own detail page — same contract as
// PointValue.vue's openDetail (name + query).
const openPeer = (pointId: string) => {
  void router.push({
    name: 'pointValueDetail',
    query: {
      deviceId: String(props.deviceId || ''),
      pointId,
    },
  });
};
</script>

<style lang="scss" scoped>
// auto-fit (not auto-fill) collapses the unused tracks: a 2-peer device fills
// the row with 2 wide tiles instead of leaving 5 of 7 columns empty, which is
// the same "half-empty row" disease the stat strip used to have. Tiles stretch
// to the body height so the fixed card height carries no dead space.
.peer-snapshot__grid {
  display: grid;
  flex: 1;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--dc3-space-2);
  min-height: 0;
}

.peer-snapshot__card {
  display: flex;
  flex-direction: column;
  height: 100%;
  cursor: pointer;
  border-color: var(--dc3-border-base);
  transition: border-color var(--dc3-duration-base) var(--dc3-ease-standard);

  &:hover,
  &:focus-visible {
    border-color: var(--el-color-primary);
  }

  :deep(.el-card__body) {
    display: flex;
    flex: 1;
    flex-direction: column;
    padding: 0;
  }
}

// Name left, value right on one baseline row — the row fills the tile width
// whatever the tile ends up being, so nothing hugs the left with dead space
// beside it.
.peer-snapshot__body {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: space-between;
  gap: var(--dc3-space-3);
  padding: var(--dc3-space-3) var(--dc3-space-4);
}

.peer-snapshot__name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  color: var(--dc3-text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.peer-snapshot__value {
  display: flex;
  align-items: baseline;
  gap: var(--dc3-space-2);
  margin-left: auto;
}

.peer-snapshot__value-text {
  font-size: 22px;
  font-weight: 680;
  color: var(--dc3-text-primary);
  letter-spacing: -0.025em;
}

.peer-snapshot__unit {
  font-size: 12px;
  color: var(--dc3-text-muted);
}

.peer-snapshot__no-value {
  font-size: 14px;
  color: var(--dc3-text-muted);
}

// Same footer strip language as DashboardCard: muted band under a hairline
// border, metadata left.
.peer-snapshot__foot {
  padding: var(--dc3-space-2) var(--dc3-space-4);
  border-top: 1px solid var(--dc3-border-base);
  background: var(--dc3-bg-muted);
  flex-shrink: 0;
}

.peer-snapshot__time {
  font-size: 12px;
  color: var(--dc3-text-muted);
}
</style>
