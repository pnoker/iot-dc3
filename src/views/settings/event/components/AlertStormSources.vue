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
  <el-card class="alert-storm" shadow="never">
    <template #header>
      <div class="alert-storm__header">
        <span class="alert-storm__title">
          {{ t('settings.event.overview.stormTitle') }}
          <span class="alert-storm__subtitle">{{
            t('settings.event.overview.stormSubtitle', { hours, min: minCount })
          }}</span>
        </span>
        <el-button :icon="Refresh" :loading="loading" circle size="small" @click="load" />
      </div>
    </template>
    <div v-if="!loading && rows.length === 0" class="alert-storm__empty">
      <el-empty :description="t('settings.event.overview.stormEmpty')" :image-size="60" />
    </div>
    <ul v-else class="alert-storm__list">
      <li v-for="row in rows" :key="`${row.source}:${row.sourceId}`" class="alert-storm__item" @click="onDrillIn(row)">
        <el-tag :type="row.source === 'device' ? 'primary' : 'warning'" size="small">
          {{ row.source === 'device' ? t('settings.event.device') : t('settings.event.driver') }}
        </el-tag>
        <span class="alert-storm__name">{{ nameFor(row) }}</span>
        <span class="alert-storm__count">
          <el-icon><Warning /></el-icon>
          {{ row.count }}
        </span>
      </li>
    </ul>
  </el-card>
</template>

<script lang="ts" setup>
  import { onMounted, reactive, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';
  import { Refresh, Warning } from '@element-plus/icons-vue';

  import { alertStormSources } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getDriverByIds } from '@/api/driver';

  interface StormRow {
    source: 'device' | 'driver';
    sourceId: number | string;
    count: number;
  }

  const props = defineProps({
    hours: { type: Number, default: 1 },
    minCount: { type: Number, default: 10 },
    limit: { type: Number, default: 10 },
  });

  const { t } = useI18n();
  const router = useRouter();

  const loading = ref(false);
  const rows = ref<StormRow[]>([]);
  const nameMap = reactive<Record<string, string>>({});

  const load = async () => {
    loading.value = true;
    try {
      const res: { data?: StormRow[] } = await alertStormSources(props.hours, props.minCount, props.limit);
      rows.value = res?.data ?? [];
      await resolveNames(rows.value);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  const resolveNames = async (batch: StormRow[]) => {
    const devIds = batch
      .filter((r) => r.source === 'device')
      .map((r) => String(r.sourceId))
      .filter((id) => id && !nameMap[`d:${id}`]);
    const drvIds = batch
      .filter((r) => r.source === 'driver')
      .map((r) => String(r.sourceId))
      .filter((id) => id && !nameMap[`r:${id}`]);
    const jobs: Promise<void>[] = [];
    if (devIds.length) {
      jobs.push(
        getDeviceByIds(devIds)
          .then((r: { data?: Record<string, { deviceName?: string }> }) => {
            const d = r?.data || {};
            for (const id of devIds) {
              if (d[id]) nameMap[`d:${id}`] = d[id].deviceName || id;
            }
          })
          .catch(() => {})
      );
    }
    if (drvIds.length) {
      jobs.push(
        getDriverByIds(drvIds)
          .then((r: { data?: Record<string, { driverName?: string }> }) => {
            const d = r?.data || {};
            for (const id of drvIds) {
              if (d[id]) nameMap[`r:${id}`] = d[id].driverName || id;
            }
          })
          .catch(() => {})
      );
    }
    await Promise.all(jobs);
  };

  const nameFor = (r: StormRow) => {
    const id = String(r.sourceId);
    return r.source === 'device' ? nameMap[`d:${id}`] || id : nameMap[`r:${id}`] || id;
  };

  // Jump into the per-source event page so the operator can triage the
  // specific flapping driver / noisy device without filtering manually.
  const onDrillIn = (row: StormRow) => {
    const name = row.source === 'device' ? 'settingsDeviceEvent' : 'settingsDriverEvent';
    router.push({ name, query: { sourceId: String(row.sourceId) } }).catch(() => {});
  };

  onMounted(load);
  defineExpose({ refresh: load });
</script>

<style lang="scss" scoped>
  .alert-storm {
    border-radius: 10px;
    height: 100%;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    :deep(.el-card__body) {
      padding: 0;
    }

    .alert-storm__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .alert-storm__title {
      font-weight: 600;
      color: #303133;
      display: inline-flex;
      align-items: baseline;
      gap: 8px;
    }

    .alert-storm__subtitle {
      font-size: 12px;
      font-weight: normal;
      color: #909399;
    }

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

    .alert-storm__empty {
      padding: 40px 0;
    }
  }
</style>
