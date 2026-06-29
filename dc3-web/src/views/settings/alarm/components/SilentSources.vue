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
    :badge="rows.length || null"
    :empty="!loading && rows.length === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.silentEmpty')"
    :loading="loading"
    :subtitle="t('settings.event.overview.silentSubtitle', {min: silentKey})"
    :title="t('settings.event.overview.silentTitle')"
    body-mode="scroll"
    class="silent-sources"
    loading-target="button"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="silentKey" :options="silentOptions" size="small" />
    </template>

    <el-table :data="rows" size="small" @row-click="onRowClick">
      <el-table-column :label="t('settings.event.overview.colDevice')" min-width="110" prop="deviceId">
        <template #default="{row}">{{ deviceName(row.deviceId) }}</template>
      </el-table-column>
      <el-table-column :label="t('settings.event.overview.colPoint')" min-width="110" prop="pointId">
        <template #default="{row}">{{ pointName(row.pointId) }}</template>
      </el-table-column>
      <el-table-column :label="t('settings.event.overview.colLastSeen')" min-width="160" prop="lastSeen">
        <template #default="{row}">{{ formatDateTime(row.lastSeen) }}</template>
      </el-table-column>
      <el-table-column :label="t('settings.event.overview.colSilentFor')" min-width="110" prop="silentSeconds">
        <template #default="{row}">
          <el-tag size="small" type="warning">{{ humanDuration(row.silentSeconds) }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import {onMounted, ref, watch} from 'vue';
  import {useI18n} from 'vue-i18n';
  import {useRouter} from 'vue-router';

  import {silentSources as apiSilentSources} from '@/api/dashboard';
  import type {SilentSource} from '@/config/types/dashboard';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
  import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
  import {useEntityNames} from '@/composables/useEntityNames';
  import {formatDateTime, humanDuration} from '@/utils/timeUtil';

  const {t} = useI18n();
  const router = useRouter();
  const {loading, run} = useAsyncLoader();
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

  const load = () =>
    run(async () => {
      const res: {data?: SilentSource[]} = await apiSilentSources(7, Number(silentKey.value), 100);
      rows.value = res?.data ?? [];
      await Promise.all([
        resolveDevices(rows.value.map((r) => r.deviceId)),
        resolvePoints(rows.value.map((r) => r.pointId)),
      ]);
    });

  watch(silentKey, load);
  onMounted(load);

  const onRowClick = (row: SilentSource) => {
    router
      .push({name: 'pointValue', query: {pointId: String(row.pointId), deviceId: String(row.deviceId)}})
      .catch(() => {});
  };

  defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
  @use '@/styles/palette.scss' as *;

  .silent-sources {
    @include clickable-rows;
  }
</style>
