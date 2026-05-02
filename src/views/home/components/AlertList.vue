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
  <el-card class="alert-list" shadow="never">
    <template #header>
      <div class="alert-list__header">
        <span class="alert-list__title">
          {{ $t('home.alertList.title') }}
          <el-badge
            v-if="stats.unconfirmed > 0"
            :value="stats.unconfirmed"
            :max="99"
            type="danger"
            class="alert-list__badge"
          />
        </span>
        <el-button :icon="Refresh" :loading="loading" circle size="small" @click="refresh" />
      </div>
    </template>

    <el-scrollbar :height="scrollHeight">
      <div v-if="!loading && rows.length === 0" class="alert-list__empty">
        <el-empty :description="$t('home.alertList.empty')" :image-size="80" />
      </div>
      <div v-for="row in rows" :key="row.id" class="alert-list__row">
        <span :class="['alert-list__level', `alert-list__level--${levelTone(row.eventTypeFlag)}`]">
          {{ levelLabel(row.eventTypeFlag) }}
        </span>
        <div class="alert-list__main">
          <div class="alert-list__line">
            <span class="alert-list__source">{{ sourceLabel(row) }}</span>
            <span class="alert-list__sep">·</span>
            <span class="alert-list__name">{{ nameFor(row) }}</span>
          </div>
          <div class="alert-list__sub">
            <span>{{ $t(row.confirmFlag === 1 ? 'home.alertList.confirmed' : 'home.alertList.unconfirmed') }}</span>
            <span>{{ formatTime(row.createTime) }}</span>
          </div>
        </div>
      </div>
    </el-scrollbar>
  </el-card>
</template>

<script lang="ts" setup>
  import { onMounted, reactive, ref } from 'vue';
  import { Refresh } from '@element-plus/icons-vue';
  import { useI18n } from 'vue-i18n';

  import { alertLatest, alertStats } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getDriverByIds } from '@/api/driver';

  interface AlertRow {
    id: number | string;
    source: 'device' | 'driver';
    sourceId: number | string;
    pointId: number | string;
    eventTypeFlag: number;
    confirmFlag: number;
    createTime: string;
  }

  const props = defineProps({
    size: { type: Number, default: 10 },
    scrollHeight: { type: String, default: '320px' },
  });

  const { t } = useI18n();

  const loading = ref(false);
  const rows = ref<AlertRow[]>([]);
  const stats = reactive({ total: 0, unconfirmed: 0 });
  const deviceMap = reactive<Record<string, string>>({});
  const driverMap = reactive<Record<string, string>>({});

  const refresh = async () => {
    loading.value = true;
    try {
      const [s, l]: any = await Promise.all([alertStats(), alertLatest(props.size)]);
      stats.total = s?.data?.total ?? 0;
      stats.unconfirmed = s?.data?.unconfirmed ?? 0;
      const data: AlertRow[] = l?.data ?? [];
      rows.value = data;
      await resolveNames(data);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  const resolveNames = async (batch: AlertRow[]) => {
    const devIds = Array.from(
      new Set(
        batch
          .filter((r) => r.source === 'device')
          .map((r) => String(r.sourceId))
          .filter((id) => id && !deviceMap[id])
      )
    );
    const drvIds = Array.from(
      new Set(
        batch
          .filter((r) => r.source === 'driver')
          .map((r) => String(r.sourceId))
          .filter((id) => id && !driverMap[id])
      )
    );
    const jobs: Promise<void>[] = [];
    if (devIds.length) {
      jobs.push(
        getDeviceByIds(devIds)
          .then((r: any) => {
            const d = r?.data || {};
            for (const id of devIds) {
              if (d[id]) deviceMap[id] = d[id].deviceName || id;
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
              if (d[id]) driverMap[id] = d[id].driverName || id;
            }
          })
          .catch(() => {})
      );
    }
    await Promise.all(jobs);
  };

  const sourceLabel = (r: AlertRow) =>
    r.source === 'device' ? t('home.alertList.sourceDevice') : t('home.alertList.sourceDriver');

  const nameFor = (r: AlertRow) => {
    const id = String(r.sourceId);
    return r.source === 'device' ? deviceMap[id] || id : driverMap[id] || id;
  };

  // Event types map roughly to: 0=INFO, 1=WARN, 2=ERROR (educated default; server-side flag is unlabeled).
  const levelLabel = (flag: number) => {
    switch (flag) {
      case 2:
      case 3:
        return t('home.alertList.levelError');
      case 1:
        return t('home.alertList.levelWarn');
      default:
        return t('home.alertList.levelInfo');
    }
  };

  const levelTone = (flag: number): 'info' | 'warn' | 'error' => {
    switch (flag) {
      case 2:
      case 3:
        return 'error';
      case 1:
        return 'warn';
      default:
        return 'info';
    }
  };

  const formatTime = (v?: string) => {
    if (!v) return '';
    const d = new Date(v.replace(' ', 'T'));
    if (Number.isNaN(d.getTime())) return v;
    return d.toLocaleString('zh-CN', { hour12: false });
  };

  onMounted(refresh);
</script>

<style lang="scss" scoped>
  .alert-list {
    border-radius: 10px;
    height: 100%;

    :deep(.el-card__header) {
      padding: 12px 16px;
    }

    :deep(.el-card__body) {
      padding: 0;
    }

    .alert-list__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .alert-list__title {
      font-weight: 600;
      color: #303133;
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }

    .alert-list__badge {
      :deep(.el-badge__content) {
        transform: none;
        position: static;
      }
    }

    .alert-list__row {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 10px 16px;
      border-bottom: 1px solid var(--el-border-color-lighter);

      &:last-child {
        border-bottom: 0;
      }
    }

    .alert-list__level {
      font-size: 10px;
      font-weight: 600;
      padding: 2px 6px;
      border-radius: 4px;
      white-space: nowrap;

      &--info {
        background: rgba(144, 147, 153, 0.14);
        color: #909399;
      }

      &--warn {
        background: rgba(230, 162, 60, 0.14);
        color: #e6a23c;
      }

      &--error {
        background: rgba(245, 108, 108, 0.14);
        color: #f56c6c;
      }
    }

    .alert-list__main {
      flex: 1;
      min-width: 0;
    }

    .alert-list__line {
      font-size: 13px;
      color: #303133;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .alert-list__source {
      font-weight: 600;
    }

    .alert-list__sep {
      margin: 0 4px;
      color: #c0c4cc;
    }

    .alert-list__name {
      color: #606266;
    }

    .alert-list__sub {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #909399;
      margin-top: 2px;
    }

    .alert-list__empty {
      padding: 40px 0;
    }
  }
</style>
