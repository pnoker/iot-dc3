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
    :empty-text="t('settings.event.overview.peerEmpty')"
    :loading="loading"
    :error="status === 'error'"
    :error-text="t('common.loadFailed')"
    :retry-text="t('common.retry')"
    :subtitle="t('settings.event.overview.peerSubtitle', {days: Number(daysKey)})"
    :title="t('settings.event.overview.peerTitle')"
    body-mode="scroll"
    class="peer-deviation"
    loading-target="button"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="daysKey" :options="daysOptions" size="small"/>
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
      <template #cell-ratio="{row}">
        <el-tag :type="row.ratio >= 5 ? 'danger' : 'warning'" size="small">
          {{ row.ratio ? `${row.ratio}×` : '—' }}
        </el-tag>
      </template>
    </responsive-record-list>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';

import {alertPeerDeviation} from '@/api/dashboard';
import type {PeerDeviation} from '@/config/types/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
import {useEntityNames} from '@/composables/useEntityNames';
import {jumpToSourceEvents} from '@/utils/jumpUtil';

const {t, locale} = useI18n();
const router = useRouter();
const {loading, run, status} = useAsyncLoader();
const {resolveDevices, resolveProfiles, deviceName, profileName} = useEntityNames();

const daysOptions = [
  {label: '1d', value: '1'},
  {label: '7d', value: '7'},
  {label: '30d', value: '30'},
];
const daysKey = ref<string>('7');

const rows = ref<PeerDeviation[]>([]);
const columns = computed(() => [
  {
    key: 'profileId',
    label: t('settings.event.overview.colProfile'),
    minWidth: 120,
    mobile: 'primary' as const,
    formatter: (row: PeerDeviation) => profileName(row.profileId),
  },
  {
    key: 'deviceId',
    label: t('settings.event.overview.colDevice'),
    minWidth: 120,
    mobile: 'detail' as const,
    formatter: (row: PeerDeviation) => deviceName(row.deviceId),
  },
  {key: 'alarmCount', label: t('settings.event.overview.colAlarmCount'), width: 100, mobile: 'detail' as const},
  {key: 'peerMedian', label: t('settings.event.overview.colPeerMedian'), width: 100, mobile: 'detail' as const},
  {key: 'ratio', label: t('settings.event.overview.colRatio'), width: 100, kind: 'custom' as const, mobile: 'detail' as const},
]);
const rowKey = (row: PeerDeviation) => `${row.profileId}:${row.deviceId}`;

const load = () =>
  run(
    async () => {
      const result: PeerDeviation[] = await alertPeerDeviation(Number(daysKey.value));
      const nextRows = result ?? [];
      await Promise.all([
        resolveDevices(nextRows.map((row) => row.deviceId)),
        resolveProfiles(nextRows.map((row) => row.profileId)),
      ]);
      return nextRows;
    },
    {apply: (nextRows) => (rows.value = nextRows)}
  );

watch(daysKey, load);
watch(locale, load);
onMounted(load);

const onRowClick = (row: PeerDeviation) => jumpToSourceEvents(router, 'device', row.deviceId);

defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
@use '@/styles/palette.scss' as *;

.peer-deviation {
  @include clickable-rows;
}
</style>
