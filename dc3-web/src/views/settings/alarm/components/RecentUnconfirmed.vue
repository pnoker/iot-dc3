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
    :empty-image-size="60"
    :empty-text="$t('settings.event.overview.noUnconfirmed')"
    :loading="loading"
    :title="$t('settings.event.overview.unconfirmedTitle')"
    body-mode="scroll"
    class="recent-unconfirmed"
    loading-target="button"
    @refresh="load"
  >
    <el-timeline class="recent-unconfirmed__timeline">
      <el-timeline-item
        v-for="row in rows"
        :key="`${row.source}:${row.id}`"
        :color="sourceColor(row.source)"
        :timestamp="formatTime(row.createTime)"
        placement="top"
      >
        <div class="recent-unconfirmed__item">
          <div class="recent-unconfirmed__line">
            <el-tag :type="sourceTagType(row.source)" size="small">
              {{ sourceLabel(row.source) }}
            </el-tag>
            <span class="recent-unconfirmed__name">{{ nameFor(row) }}</span>
          </div>
          <div v-if="row.message" :title="row.message" class="recent-unconfirmed__message">{{ row.message }}</div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {onMounted, ref} from 'vue';
import {useI18n} from 'vue-i18n';

import {alertPage} from '@/api/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import {useEntityNames} from '@/composables/useEntityNames';
import type {AlertSource} from '@/config/types/dashboard';

interface Row {
  id: number | string;
  source: AlertSource;
  sourceId: number | string;
  createTime: string;
  message?: string;
}

const {t} = useI18n();
const loading = ref(false);
const rows = ref<Row[]>([]);
const {resolveBySource, nameBySource} = useEntityNames();

const load = async () => {
  loading.value = true;
  try {
    const res: { data?: { records?: Row[] } } = await alertPage({confirmFlag: 0, current: 1, size: 5});
    const data: Row[] = res?.data?.records ?? [];
    rows.value = data;
    await resolveBySource(data);
  } catch {
    // handled globally
  } finally {
    loading.value = false;
  }
};

const nameFor = (r: Row) => nameBySource(r.source, r.sourceId);

const sourceTagType = (s: AlertSource) => (s === 'device' ? 'primary' : s === 'driver' ? 'warning' : 'success');
const sourceColor = (s: AlertSource) => (s === 'device' ? '#409eff' : s === 'driver' ? '#e6a23c' : '#67c23a');
const sourceLabel = (s: AlertSource) => {
  if (s === 'point') return t('settings.event.sourcePoint');
  if (s === 'driver') return t('settings.event.driver');
  return t('settings.event.device');
};

const formatTime = (v?: string) => {
  if (!v) return '';
  const d = new Date(v.replace(' ', 'T'));
  if (Number.isNaN(d.getTime())) return v;
  return d.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
};

onMounted(load);
defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
.recent-unconfirmed {
  .recent-unconfirmed__timeline {
    padding: 12px 16px 0;
  }

  :deep(.el-timeline-item__timestamp) {
    font-size: 12px;
    color: #909399;
    margin-bottom: 2px;
  }

  :deep(.el-timeline-item__tail) {
    border-left-color: rgba(64, 158, 255, 0.25);
  }

  :deep(.el-timeline-item__node) {
    box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
  }

  .recent-unconfirmed__item {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .recent-unconfirmed__line {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .recent-unconfirmed__name {
    font-size: 13px;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .recent-unconfirmed__message {
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
