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
  <el-card class="recent-unconfirmed" shadow="never">
    <template #header>
      <div class="recent-unconfirmed__header">
        <span class="recent-unconfirmed__title">
          {{ $t('settings.event.overview.unconfirmedTitle') }}
          <el-badge
            v-if="rows.length > 0"
            :value="rows.length"
            :max="99"
            type="danger"
            class="recent-unconfirmed__badge"
          />
        </span>
        <el-button :icon="Refresh" :loading="loading" circle size="small" @click="load" />
      </div>
    </template>

    <el-scrollbar :height="scrollHeight">
      <div v-if="!loading && rows.length === 0" class="recent-unconfirmed__empty">
        <el-empty :description="$t('settings.event.overview.noUnconfirmed')" :image-size="60" />
      </div>
      <div v-for="row in rows" :key="`${row.source}:${row.id}`" class="recent-unconfirmed__item">
        <div class="recent-unconfirmed__item-header">
          <el-tag :type="row.source === 'device' ? 'primary' : 'warning'" size="small">
            {{ row.source === 'device' ? $t('settings.event.device') : $t('settings.event.driver') }}
          </el-tag>
          <span class="recent-unconfirmed__name">{{ nameFor(row) }}</span>
          <span class="recent-unconfirmed__time">{{ formatTime(row.createTime) }}</span>
        </div>
        <div v-if="row.message" class="recent-unconfirmed__message">{{ row.message }}</div>
      </div>
    </el-scrollbar>
  </el-card>
</template>

<script lang="ts" setup>
  import { onMounted, reactive, ref } from 'vue';
  import { Refresh } from '@element-plus/icons-vue';

  import { alertPage } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getDriverByIds } from '@/api/driver';

  interface Row {
    id: number | string;
    source: 'device' | 'driver';
    sourceId: number | string;
    createTime: string;
    message?: string;
  }

  defineProps({
    scrollHeight: { type: String, default: '320px' },
  });

  const loading = ref(false);
  const rows = ref<Row[]>([]);
  const nameMap = reactive<Record<string, string>>({});

  const load = async () => {
    loading.value = true;
    try {
      const res: any = await alertPage({ confirmFlag: 0, current: 1, size: 10 });
      const data: Row[] = res?.data?.records ?? [];
      rows.value = data;
      await resolveNames(data);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  const resolveNames = async (batch: Row[]) => {
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
          .then((r: any) => {
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
          .then((r: any) => {
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

  const nameFor = (r: Row) => {
    const id = String(r.sourceId);
    return r.source === 'device' ? nameMap[`d:${id}`] || id : nameMap[`r:${id}`] || id;
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
  defineExpose({ refresh: load });
</script>

<style lang="scss" scoped>
  .recent-unconfirmed {
    border-radius: 10px;
    height: 100%;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    :deep(.el-card__body) {
      padding: 0;
    }

    .recent-unconfirmed__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .recent-unconfirmed__title {
      font-weight: 600;
      color: #303133;
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }

    .recent-unconfirmed__badge {
      :deep(.el-badge__content) {
        transform: none;
        position: static;
      }
    }

    .recent-unconfirmed__item {
      padding: 10px 16px;
      border-bottom: 1px solid var(--el-border-color-lighter);

      &:last-child {
        border-bottom: none;
      }
    }

    .recent-unconfirmed__item-header {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .recent-unconfirmed__name {
      font-size: 13px;
      color: #606266;
      flex: 1;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .recent-unconfirmed__time {
      font-size: 12px;
      color: #909399;
      flex-shrink: 0;
    }

    .recent-unconfirmed__message {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .recent-unconfirmed__empty {
      padding: 40px 0;
    }
  }
</style>
