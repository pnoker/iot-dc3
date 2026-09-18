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
    :empty="status === 'success' && report.totalPoints === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.coverageEmpty')"
    :loading="loading"
    :error="status === 'error'"
    :error-text="t('common.loadFailed')"
    :retry-text="t('common.retry')"
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

    <responsive-record-list
      v-if="report.items.length"
      :columns="columns"
      :loading="loading"
      :rows="report.items"
      :status="status"
      embedded
      openable
      :row-key="rowKey"
      @open="onRowClick"
      @retry="load"
    />
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, onMounted, reactive, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';

import {coverageGap} from '@/api/dashboard';
import type {CoverageGap, CoverageGapItem} from '@/config/types/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
import {useEntityNames} from '@/composables/useEntityNames';
import {jumpToEntity} from '@/utils/jumpUtil';

const {t, locale} = useI18n();
const router = useRouter();
const {loading, run, status} = useAsyncLoader();
const {resolvePoints, resolveProfiles, pointName, profileName} = useEntityNames();

const report = reactive<CoverageGap>({totalPoints: 0, missingPoints: 0, items: []});
const columns = computed(() => [
  {
    key: 'pointId',
    label: t('settings.event.overview.colPoint'),
    minWidth: 130,
    mobile: 'primary' as const,
    formatter: (row: CoverageGapItem) => pointName(row.pointId),
  },
  {
    key: 'profileId',
    label: t('settings.event.overview.colProfile'),
    minWidth: 130,
    mobile: 'detail' as const,
    formatter: (row: CoverageGapItem) => profileName(row.profileId),
  },
]);
const rowKey = (row: CoverageGapItem) => `${row.profileId}:${row.pointId}`;

const coveragePercent = computed(() => {
  if (report.totalPoints === 0) return 0;
  return Math.round(((report.totalPoints - report.missingPoints) / report.totalPoints) * 100);
});

const coverageColor = computed(() => {
  const p = coveragePercent.value;
  if (p >= 90) return 'var(--el-color-success)';
  if (p >= 70) return 'var(--el-color-warning)';
  return 'var(--el-color-danger)';
});

const subtitleText = computed(() =>
  t('settings.event.overview.coverageSubtitle', {
    missing: report.missingPoints,
    total: report.totalPoints,
  })
);

const load = () =>
  run(
    async () => {
      const result: CoverageGap = await coverageGap(100);
      const nextReport = result ?? {totalPoints: 0, missingPoints: 0, items: []};
      await Promise.all([
        resolvePoints(nextReport.items.map((row) => row.pointId)),
        resolveProfiles(nextReport.items.map((row) => row.profileId)),
      ]);
      return nextReport;
    },
    {apply: (nextReport) => Object.assign(report, nextReport)}
  );

onMounted(load);
watch(locale, load);

const onRowClick = (row: CoverageGapItem) => jumpToEntity(router, 'point', row.pointId);

defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
@use '@/styles/palette.scss' as *;

.coverage-gap {
  .coverage-gap__summary {
    display: flex;
    align-items: center;
    gap: var(--dc3-space-6);
    padding: var(--dc3-space-4);
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .coverage-gap__nums {
    display: flex;
    flex-direction: column;
    gap: var(--dc3-space-1);
  }

  .coverage-gap__num {
    display: flex;
    align-items: baseline;
    gap: var(--dc3-space-2);
    font-size: 13px;

    &--gap .coverage-gap__value {
      color: var(--el-color-danger);
    }
  }

  .coverage-gap__label {
    color: var(--dc3-text-muted);
    min-width: 48px;
  }

  .coverage-gap__value {
    color: var(--dc3-text-primary);
    font-weight: 600;
    font-size: 15px;
  }

  @include clickable-rows;
}

@media (max-width: $breakpoint-xs-max) {
  .coverage-gap .coverage-gap__summary {
    align-items: flex-start;
    gap: var(--dc3-space-3);
    padding: var(--dc3-space-3);
  }
}
</style>
