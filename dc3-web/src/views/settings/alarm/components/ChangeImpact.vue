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
    :empty="status === 'success' && rows.length === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.changeImpactEmpty')"
    :error="status === 'error'"
    :error-text="t('common.loadFailed')"
    :loading="loading"
    :retry-text="t('common.retry')"
    :subtitle="t('settings.event.overview.changeImpactSubtitle', {days: Number(daysKey)})"
    :title="t('settings.event.overview.changeImpactTitle')"
    body-mode="scroll"
    class="change-impact"
    loading-target="button"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="daysKey" :options="daysOptions" size="small"/>
    </template>

    <el-timeline class="change-impact__timeline">
      <el-timeline-item
        v-for="row in rows"
        :key="`${row.kind}:${row.entityId}:${row.operateTime}`"
        :color="resolveDashboardColour(row.kind)"
        :timestamp="formatDateTime(row.operateTime)"
        placement="top"
      >
        <button class="change-impact__row" type="button" @click="onJump(row)">
          <el-tag :type="tagTypeFor(row.kind)" size="small">{{ kindLabel(row.kind) }}</el-tag>
          <span class="change-impact__name">{{ entityName(row) }}</span>
        </button>
      </el-timeline-item>
    </el-timeline>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {onMounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';

import {alertChangeImpact} from '@/api/dashboard';
import type {ChangeImpact} from '@/config/types/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
import {useEntityNames} from '@/composables/useEntityNames';
import {resolveDashboardColour} from '@/config/constant/palette';
import {jumpToEntity} from '@/utils/jumpUtil';
import {formatDateTime} from '@/utils/timeUtil';

const {t, locale} = useI18n();
const router = useRouter();
const {loading, run, status} = useAsyncLoader();
const {resolveDevices, resolveDrivers, resolveProfiles, deviceName, driverName, profileName} = useEntityNames();

const daysOptions = [
  {label: '1d', value: '1'},
  {label: '7d', value: '7'},
  {label: '30d', value: '30'},
];
const daysKey = ref<string>('7');

const rows = ref<ChangeImpact[]>([]);

const load = () =>
  run(
    async () => {
      const result: ChangeImpact[] = await alertChangeImpact(Number(daysKey.value), 30);
      const nextRows = result ?? [];
      await Promise.all([
        resolveDrivers(nextRows.filter((row) => row.kind === 'driver').map((row) => row.entityId)),
        resolveDevices(nextRows.filter((row) => row.kind === 'device').map((row) => row.entityId)),
        resolveProfiles(nextRows.filter((row) => row.kind === 'profile').map((row) => row.entityId)),
      ]);
      return nextRows;
    },
    {apply: (nextRows) => (rows.value = nextRows)}
  );

watch(daysKey, load);
watch(locale, load);
onMounted(load);

const entityName = (r: ChangeImpact) => {
  if (r.kind === 'driver') return driverName(r.entityId);
  if (r.kind === 'device') return deviceName(r.entityId);
  return profileName(r.entityId);
};

const kindLabel = (k: string) => {
  if (k === 'driver') return t('settings.event.overview.kindDriver');
  if (k === 'device') return t('settings.event.overview.kindDevice');
  return t('settings.event.overview.kindProfile');
};

const tagTypeFor = (k: string): 'primary' | 'warning' | 'success' | 'info' => {
  if (k === 'driver') return 'info';
  if (k === 'device') return 'primary';
  return 'warning';
};

const onJump = (r: ChangeImpact) => jumpToEntity(router, r.kind, r.entityId);

defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
.change-impact {
  .change-impact__timeline {
    padding: var(--dc3-space-3) var(--dc3-space-4) 0;
  }

  .change-impact__row {
    appearance: none;
    display: inline-flex;
    align-items: center;
    min-height: var(--dc3-touch-target);
    gap: var(--dc3-space-2);
    padding: 0 var(--dc3-space-2);
    border: 0;
    border-radius: var(--dc3-radius-md);
    background: transparent;
    font: inherit;
    cursor: pointer;

    &:hover .change-impact__name,
    &:focus-visible .change-impact__name {
      color: var(--el-color-primary);
    }

    &:focus-visible {
      outline: none;
      box-shadow: var(--dc3-focus-ring);
    }
  }

  .change-impact__name {
    font-size: 13px;
    color: var(--dc3-text-primary);
    font-weight: 500;
  }
}
</style>
