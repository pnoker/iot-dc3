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
    :empty="!loading && report.totalPoints === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.coverageEmpty')"
    :loading="loading"
    :subtitle="subtitleText"
    :title="t('settings.event.overview.coverageTitle')"
    body-mode="scroll"
    class="coverage-gap"
    loading-target="button"
    @refresh="load"
  >
    <div class="coverage-gap__summary">
      <el-progress :color="coverageColor" :percentage="coveragePercent" :width="100" type="dashboard"/>
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
        <template #default="{row}">{{ pointName(row.pointId) }}</template>
      </el-table-column>
      <el-table-column :label="t('settings.event.overview.colProfile')" min-width="130">
        <template #default="{row}">{{ profileName(row.profileId) }}</template>
      </el-table-column>
    </el-table>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';

import {coverageGap} from '@/api/dashboard';
import type {CoverageGap, CoverageGapItem} from '@/config/types/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
import {useEntityNames} from '@/composables/useEntityNames';
import {jumpToEntity} from '@/utils/jumpUtil';

const {t} = useI18n();
const router = useRouter();
const {loading, run} = useAsyncLoader();
const {resolvePoints, resolveProfiles, pointName, profileName} = useEntityNames();

const report = reactive<CoverageGap>({totalPoints: 0, missingPoints: 0, items: []});

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

const load = () =>
  run(async () => {
    const res: { data?: CoverageGap } = await coverageGap(100);
    Object.assign(report, res?.data ?? {totalPoints: 0, missingPoints: 0, items: []});
    await Promise.all([
      resolvePoints(report.items.map((r) => r.pointId)),
      resolveProfiles(report.items.map((r) => r.profileId)),
    ]);
  });

onMounted(load);

const onRowClick = (row: CoverageGapItem) => jumpToEntity(router, 'point', row.pointId);

defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
@use '@/styles/palette.scss' as *;

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

  @include clickable-rows;
}
</style>
