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
  <dashboard-card
    class="coverage-gap"
    :title="t('settings.event.overview.coverageTitle')"
    :subtitle="subtitleText"
    :loading="loading"
    loading-target="button"
    :empty="!loading && report.totalPoints === 0"
    :empty-text="t('settings.event.overview.coverageEmpty')"
    :empty-image-size="60"
    body-mode="scroll"
    @refresh="load"
  >
    <div class="coverage-gap__summary">
      <el-progress type="dashboard" :percentage="coveragePercent" :color="coverageColor" :width="100" />
      <div class="coverage-gap__nums">
        <div class="coverage-gap__num">
          <span class="coverage-gap__label">{{ t('settings.event.overview.coverageCovered') }}</span>
          <span class="coverage-gap__value">{{ report.totalPoints - report.missingPoints }}</span>
        </div>
        <div class="coverage-gap__num coverage-gap__num--gap">
          <span class="coverage-gap__label">{{ t('settings.event.overview.coverageMissing') }}</span>
          <span class="coverage-gap__value">{{ report.missingPoints }}</span>
        </div>
        <div class="coverage-gap__num">
          <span class="coverage-gap__label">{{ t('settings.event.overview.coverageTotal') }}</span>
          <span class="coverage-gap__value">{{ report.totalPoints }}</span>
        </div>
      </div>
    </div>

    <el-table v-if="report.items.length" :data="report.items" size="small" @row-click="onRowClick">
      <el-table-column :label="t('settings.event.overview.colPoint')" min-width="130">
        <template #default="{ row }">{{ pointName(row) }}</template>
      </el-table-column>
      <el-table-column :label="t('settings.event.overview.colProfile')" min-width="130">
        <template #default="{ row }">{{ profileName(row) }}</template>
      </el-table-column>
    </el-table>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';

  import { coverageGap } from '@/api/dashboard';
  import type { CoverageGap } from '@/api/dashboard';
  import { getPointByIds } from '@/api/point';
  import { getProfileByIds } from '@/api/profile';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';

  const { t } = useI18n();
  const router = useRouter();

  const loading = ref(false);
  const report = reactive<CoverageGap>({ totalPoints: 0, missingPoints: 0, items: [] });
  const nameMap = reactive<{ points: Record<string, string>; profiles: Record<string, string> }>({
    points: {},
    profiles: {},
  });

  const coveragePercent = computed(() => {
    if (report.totalPoints === 0) return 0;
    return Math.round(((report.totalPoints - report.missingPoints) / report.totalPoints) * 100);
  });

  const coverageColor = computed(() => {
    const p = coveragePercent.value;
    if (p >= 90) return '#67c23a';
    if (p >= 70) return '#e6a23c';
    return '#f56c6c';
  });

  const subtitleText = computed(() =>
    t('settings.event.overview.coverageSubtitle', {
      missing: report.missingPoints,
      total: report.totalPoints,
    })
  );

  const load = async () => {
    loading.value = true;
    try {
      const res: { data?: CoverageGap } = await coverageGap(100);
      Object.assign(report, res?.data ?? { totalPoints: 0, missingPoints: 0, items: [] });
      await resolveNames();
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  const resolveNames = async () => {
    const ptIds = Array.from(new Set(report.items.map((r) => String(r.pointId)))).filter((id) => !nameMap.points[id]);
    const profIds = Array.from(new Set(report.items.map((r) => String(r.profileId)))).filter(
      (id) => !nameMap.profiles[id]
    );
    const jobs: Promise<void>[] = [];
    if (ptIds.length) {
      jobs.push(
        getPointByIds(ptIds)
          .then((r: { data?: Record<string, { pointName?: string }> }) => {
            const d = r?.data || {};
            for (const id of ptIds) if (d[id]) nameMap.points[id] = d[id].pointName || id;
          })
          .catch(() => {})
      );
    }
    if (profIds.length) {
      jobs.push(
        getProfileByIds(profIds)
          .then((r: { data?: Record<string, { profileName?: string }> }) => {
            const d = r?.data || {};
            for (const id of profIds) if (d[id]) nameMap.profiles[id] = d[id].profileName || id;
          })
          .catch(() => {})
      );
    }
    await Promise.all(jobs);
  };

  const pointName = (r: { pointId: number | string }) => nameMap.points[String(r.pointId)] || String(r.pointId);
  const profileName = (r: { profileId: number | string }) =>
    nameMap.profiles[String(r.profileId)] || String(r.profileId);

  const onRowClick = (row: { pointId: number | string }) => {
    router.push({ name: 'pointValue', query: { pointId: String(row.pointId) } }).catch(() => {});
  };

  onMounted(load);
</script>

<style lang="scss" scoped>
  .coverage-gap {
    .coverage-gap__summary {
      display: flex;
      align-items: center;
      gap: 24px;
      padding: 16px;
      border-bottom: 1px solid var(--el-border-color-lighter);
    }
    .coverage-gap__nums {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .coverage-gap__num {
      display: flex;
      align-items: baseline;
      gap: 10px;
      font-size: 13px;
      &--gap .coverage-gap__value {
        color: #f56c6c;
      }
    }
    .coverage-gap__label {
      color: #909399;
      min-width: 48px;
    }
    .coverage-gap__value {
      color: #303133;
      font-weight: 600;
      font-size: 15px;
    }
    :deep(.el-table__row) {
      cursor: pointer;
    }
  }
</style>
