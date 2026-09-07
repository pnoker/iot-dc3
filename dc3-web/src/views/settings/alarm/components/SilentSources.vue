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
    :empty="status === 'success' && rows.length === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.silentEmpty')"
    :loading="loading"
    :error="status === 'error'"
    :error-text="t('common.loadFailed')"
    :retry-text="t('common.retry')"
    :subtitle="t('settings.event.overview.silentSubtitle', {min: silentKey})"
    :title="t('settings.event.overview.silentTitle')"
    body-mode="scroll"
    class="silent-sources"
    loading-target="button"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="silentKey" :options="silentOptions" size="small"/>
    </template>

    <responsive-record-list
      :columns="columns"
      :loading="loading"
      :rows="rows"
      :status="status"
      embedded
      openable
      :row-key="rowKey"
      @open="onRowClick"
      @retry="load"
    >
      <template #cell-silentSeconds="{row}">
        <el-tag size="small" type="warning">{{ humanDuration(row.silentSeconds) }}</el-tag>
      </template>
    </responsive-record-list>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';

import {silentSources as apiSilentSources} from '@/api/dashboard';
import type {SilentSource} from '@/config/types/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
import {useEntityNames} from '@/composables/useEntityNames';
import {formatDateTime, humanDuration} from '@/utils/timeUtil';

const {t, locale} = useI18n();
const router = useRouter();
const {loading, run, status} = useAsyncLoader();
const {resolveDevices, resolvePoints, deviceName, pointName} = useEntityNames();

// silentKey is the threshold (minutes) — "no sample within the last N min
// for a baseline-active point" becomes flagged.
const silentOptions = [
  {label: '15m', value: '15'},
  {label: '1h', value: '60'},
  {label: '6h', value: '360'},
];
const silentKey = ref<string>('15');

const rows = ref<SilentSource[]>([]);
const columns = computed(() => [
  {
    key: 'deviceId',
    label: t('settings.event.overview.colDevice'),
    minWidth: 110,
    mobile: 'primary' as const,
    formatter: (row: SilentSource) => deviceName(row.deviceId),
  },
  {
    key: 'pointId',
    label: t('settings.event.overview.colPoint'),
    minWidth: 110,
    mobile: 'detail' as const,
    formatter: (row: SilentSource) => pointName(row.pointId),
  },
  {
    key: 'lastSeen',
    label: t('settings.event.overview.colLastSeen'),
    minWidth: 160,
    mobile: 'detail' as const,
    formatter: (row: SilentSource) => formatDateTime(row.lastSeen),
  },
  {
    key: 'silentSeconds',
    label: t('settings.event.overview.colSilentFor'),
    minWidth: 110,
    kind: 'custom' as const,
    mobile: 'detail' as const,
  },
]);
const rowKey = (row: SilentSource) => `${row.deviceId}:${row.pointId}`;

const load = () =>
  run(
    async () => {
      const result: SilentSource[] = await apiSilentSources(7, Number(silentKey.value), 100);
      const nextRows = result ?? [];
      await Promise.all([
        resolveDevices(nextRows.map((row) => row.deviceId)),
        resolvePoints(nextRows.map((row) => row.pointId)),
      ]);
      return nextRows;
    },
    {apply: (nextRows) => (rows.value = nextRows)}
  );

watch(silentKey, load);
watch(locale, load);
onMounted(load);

const onRowClick = (row: SilentSource) => {
  router
    .push({name: 'pointValue', query: {pointId: String(row.pointId), deviceId: String(row.deviceId)}})
    .catch(() => {
    });
};

defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
@use '@/styles/palette.scss' as *;

.silent-sources {
  @include clickable-rows;
}
</style>
