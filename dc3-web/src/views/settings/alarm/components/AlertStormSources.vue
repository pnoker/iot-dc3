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
  <dashboard-card
    :empty="!loading && rows.length === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.stormEmpty')"
    :loading="loading"
    :subtitle="t('settings.event.overview.stormSubtitle', {hours: window.hours, min: window.minCount})"
    :title="t('settings.event.overview.stormTitle')"
    body-mode="scroll"
    class="alert-storm"
    loading-target="button"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="windowKey" :options="windowOptions" size="small"/>
    </template>

    <ul class="alert-storm__list">
      <li v-for="row in rows" :key="`${row.source}:${row.sourceId}`" class="alert-storm__item" @click="onDrillIn(row)">
        <el-tag :type="sourceTagType(row.source)" size="small">
          {{ sourceLabel(row.source) }}
        </el-tag>
        <span class="alert-storm__name">{{ nameFor(row) }}</span>
        <span class="alert-storm__count">
          <el-icon><Warning/></el-icon>
          {{ row.count }}
        </span>
      </li>
    </ul>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';
import {Warning} from '@element-plus/icons-vue';

import {alertStormSources} from '@/api/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import {useEntityNames} from '@/composables/useEntityNames';
import {jumpToSourceEvents} from '@/utils/jumpUtil';
import type {AlertSource} from '@/config/types/dashboard';

interface StormRow {
  source: AlertSource;
  sourceId: number | string;
  count: number;
}

const props = defineProps({
  limit: {type: Number, default: 10},
});

const {t} = useI18n();
const router = useRouter();
const {resolveBySource, nameBySource} = useEntityNames();

// Storm is "high frequency within a short window", so the threshold has
// to scale with the window: a source hitting 10 alarms in 1h is noisy,
// but 10 in 24h is noise you'd ignore. Preset pairs below keep the
// relative severity consistent across the three spans.
type WindowKey = '1h' | '6h' | '24h';
const WINDOW_SPECS: Record<WindowKey, { hours: number; minCount: number }> = {
  '1h': {hours: 1, minCount: 10},
  '6h': {hours: 6, minCount: 30},
  '24h': {hours: 24, minCount: 100},
};
const windowOptions = [
  {label: '1h', value: '1h' as WindowKey},
  {label: '6h', value: '6h' as WindowKey},
  {label: '24h', value: '24h' as WindowKey},
];
// Default to 24h so the page is populated out of the box on fresh
// tenants — 1h would frequently be empty when the fleet is quiet.
const windowKey = ref<WindowKey>('24h');
const window = computed(() => WINDOW_SPECS[windowKey.value]);

const loading = ref(false);
const rows = ref<StormRow[]>([]);

const load = async () => {
  loading.value = true;
  try {
    const {hours, minCount} = window.value;
    const res: { data?: StormRow[] } = await alertStormSources(hours, minCount, props.limit);
    rows.value = res?.data ?? [];
    await resolveBySource(rows.value);
  } catch {
    // handled globally
  } finally {
    loading.value = false;
  }
};

watch(windowKey, load);

const nameFor = (r: StormRow) => nameBySource(r.source, r.sourceId);

const onDrillIn = (row: StormRow) => jumpToSourceEvents(router, row.source, row.sourceId);

const sourceTagType = (s: AlertSource) => (s === 'device' ? 'primary' : s === 'driver' ? 'warning' : 'success');
const sourceLabel = (s: AlertSource) => {
  if (s === 'point') return t('settings.event.sourcePoint');
  if (s === 'driver') return t('settings.event.driver');
  return t('settings.event.device');
};

onMounted(load);
defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
.alert-storm {
  .alert-storm__list {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  .alert-storm__item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 16px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    cursor: pointer;
    transition: background-color 0.12s ease;

    &:hover {
      background: #fafafa;
    }

    &:last-child {
      border-bottom: none;
    }
  }

  .alert-storm__name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
    color: #303133;
  }

  .alert-storm__count {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: #f56c6c;
    font-weight: 600;
    font-size: 13px;
  }
}
</style>
