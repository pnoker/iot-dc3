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
    :empty="!loading && rows.length === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.peerEmpty')"
    :loading="loading"
    :subtitle="t('settings.event.overview.peerSubtitle', {days: Number(daysKey)})"
    :title="t('settings.event.overview.peerTitle')"
    body-mode="scroll"
    class="peer-deviation"
    loading-target="button"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="daysKey" :options="daysOptions" size="small" />
    </template>

    <el-table :data="rows" size="small" @row-click="onRowClick">
      <el-table-column :label="t('settings.event.overview.colProfile')" min-width="120">
        <template #default="{row}">{{ profileName(row.profileId) }}</template>
      </el-table-column>
      <el-table-column :label="t('settings.event.overview.colDevice')" min-width="120">
        <template #default="{row}">{{ deviceName(row.deviceId) }}</template>
      </el-table-column>
      <el-table-column
        :label="t('settings.event.overview.colAlarmCount')"
        align="right"
        prop="alarmCount"
        width="100"
      />
      <el-table-column
        :label="t('settings.event.overview.colPeerMedian')"
        align="right"
        prop="peerMedian"
        width="100"
      />
      <el-table-column :label="t('settings.event.overview.colRatio')" align="right" width="100">
        <template #default="{row}">
          <el-tag :type="row.ratio >= 5 ? 'danger' : 'warning'" size="small">
            {{ row.ratio ? `${row.ratio}×` : '—' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import {onMounted, ref, watch} from 'vue';
  import {useI18n} from 'vue-i18n';
  import {useRouter} from 'vue-router';

  import {alertPeerDeviation} from '@/api/dashboard';
  import type {PeerDeviation} from '@/config/types/dashboard';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
  import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
  import {useEntityNames} from '@/composables/useEntityNames';
  import {jumpToSourceEvents} from '@/utils/jumpUtil';

  const {t} = useI18n();
  const router = useRouter();
  const {loading, run} = useAsyncLoader();
  const {resolveDevices, resolveProfiles, deviceName, profileName} = useEntityNames();

  const daysOptions = [
    {label: '1d', value: '1'},
    {label: '7d', value: '7'},
    {label: '30d', value: '30'},
  ];
  const daysKey = ref<string>('7');

  const rows = ref<PeerDeviation[]>([]);

  const load = () =>
    run(async () => {
      const res: {data?: PeerDeviation[]} = await alertPeerDeviation(Number(daysKey.value));
      rows.value = res?.data ?? [];
      await Promise.all([
        resolveDevices(rows.value.map((r) => r.deviceId)),
        resolveProfiles(rows.value.map((r) => r.profileId)),
      ]);
    });

  watch(daysKey, load);
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
