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
    :badge="rows.length || null"
    :empty="!loading && rows.length === 0"
    :empty-text="$t('home.alertList.empty')"
    :loading="loading"
    :title="$t('home.alertList.title')"
    body-mode="scroll"
    class="alert-list"
    loading-target="button"
    @refresh="refresh"
  >
    <div v-for="group in groupedRows" :key="group.date" class="alert-list__group">
      <div class="alert-list__date">{{ group.date }}</div>
      <el-timeline>
        <el-timeline-item
          v-for="row in group.items"
          :key="row.id"
          :hollow="row.confirmFlag === 'CONFIRMED'"
          :timestamp="formatClock(row.createTime)"
          :type="timelineColour(row.eventTypeFlag)"
          placement="top"
        >
          <div class="alert-list__body">
            <div class="alert-list__tags">
              <el-tag :type="tagType(row.eventTypeFlag)" size="small">
                {{ levelLabel(row.eventTypeFlag) }}
              </el-tag>
              <el-tag :type="sourceTagType(row.source)" size="small">
                {{ sourceLabel(row) }}
              </el-tag>
              <span class="alert-list__name">{{ nameFor(row) }}</span>
              <el-tag v-if="row.confirmFlag === 'CONFIRMED'" effect="plain" size="small" type="success">
                {{ $t('common.confirmed') }}
              </el-tag>
            </div>
            <div v-if="row.message" :title="row.message" class="alert-list__message">{{ row.message }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, ref} from 'vue';
import {useI18n} from 'vue-i18n';

import {alertLatest, alertStats} from '@/api/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import {useEntityNames} from '@/composables/useEntityNames';
import type {AlertSource} from '@/config/types/dashboard';

interface AlertRow {
  id: number | string;
  source: AlertSource;
  sourceId: number | string;
  pointId: number | string;
  eventTypeFlag: number;
  confirmFlag: string;
  createTime: string;
  message?: string;
}

const props = defineProps({
  size: {type: Number, default: 10},
});

const {t} = useI18n();

const loading = ref(false);
const rows = ref<AlertRow[]>([]);
const stats = reactive({total: 0, unconfirmed: 0});
const {resolveBySource, nameBySource} = useEntityNames();

const refresh = async () => {
  loading.value = true;
  try {
    const [s, l]: any = await Promise.all([alertStats(), alertLatest(props.size)]);
    stats.total = (s?.data?.driverAlerts ?? 0) + (s?.data?.deviceAlerts ?? 0);
    stats.unconfirmed = (s?.data?.driverUnconfirmed ?? 0) + (s?.data?.deviceUnconfirmed ?? 0);
    const data: AlertRow[] = l?.data ?? [];
    rows.value = data;
    await resolveBySource(data);
  } catch {
    // handled globally
  } finally {
    loading.value = false;
  }
};

// Group the flat row list by YYYY-MM-DD so the timeline has day headings.
const groupedRows = computed(() => {
  const byDate = new Map<string, AlertRow[]>();
  for (const row of rows.value) {
    const d = parseTime(row.createTime);
    const key = d ? d.toLocaleDateString() : '-';
    if (!byDate.has(key)) byDate.set(key, []);
    byDate.get(key)!.push(row);
  }
  // Map preserves insertion order; rows come back newest-first so date
  // groups also land newest-first without extra sorting.
  return Array.from(byDate.entries()).map(([date, items]) => ({date, items}));
});

const sourceLabel = (r: AlertRow) => {
  if (r.source === 'point') return t('home.alertList.sourcePoint');
  if (r.source === 'driver') return t('home.alertList.sourceDriver');
  return t('home.alertList.sourceDevice');
};

const nameFor = (r: AlertRow) => nameBySource(r.source, r.sourceId);

const sourceTagType = (s: AlertSource) => (s === 'device' ? 'primary' : s === 'driver' ? 'warning' : 'success');

const levelLabel = (flag: number) => {
  switch (flag) {
    case 2:
    case 3:
      return t('common.levelError');
    case 1:
      return t('common.levelWarn');
    default:
      return t('common.levelInfo');
  }
};

const tagType = (flag: number): 'info' | 'warning' | 'danger' => {
  if (flag >= 2) return 'danger';
  if (flag === 1) return 'warning';
  return 'info';
};

const timelineColour = (flag: number): 'primary' | 'warning' | 'danger' | 'info' => {
  if (flag >= 2) return 'danger';
  if (flag === 1) return 'warning';
  return 'primary';
};

const parseTime = (v?: string): Date | null => {
  if (!v) return null;
  const d = new Date(v.replace(' ', 'T'));
  return Number.isNaN(d.getTime()) ? null : d;
};

const formatClock = (v?: string) => {
  const d = parseTime(v);
  if (!d) return v || '';
  return d.toLocaleTimeString('zh-CN', {hour12: false});
};

onMounted(refresh);
defineExpose({refresh});
</script>

<style lang="scss" scoped>
.alert-list {
  .alert-list__group {
    padding: 10px 20px 0;

    &:last-child {
      padding-bottom: 10px;
    }
  }

  .alert-list__date {
    font-size: 12px;
    font-weight: 600;
    color: #909399;
    padding: 4px 0 6px;
    border-bottom: 1px dashed var(--el-border-color-lighter);
    margin-bottom: 8px;
  }

  :deep(.el-timeline) {
    padding-left: 4px;
  }

  :deep(.el-timeline-item__timestamp) {
    font-size: 12px;
    color: #909399;
    margin-bottom: 2px;
  }

  .alert-list__body {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .alert-list__tags {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
  }

  .alert-list__name {
    font-size: 13px;
    color: #606266;
  }

  .alert-list__message {
    font-size: 12px;
    color: #909399;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    word-break: break-word;
  }
}
</style>
