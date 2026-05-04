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
    class="peer-deviation"
    :title="t('settings.event.overview.peerTitle')"
    :subtitle="t('settings.event.overview.peerSubtitle', { days: Number(daysKey) })"
    :loading="loading"
    loading-target="button"
    :empty="!loading && rows.length === 0"
    :empty-text="t('settings.event.overview.peerEmpty')"
    :empty-image-size="60"
    body-mode="scroll"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="daysKey" :options="daysOptions" size="small" />
    </template>

    <el-table :data="rows" size="small" @row-click="onRowClick">
      <el-table-column :label="t('settings.event.overview.colProfile')" min-width="120">
        <template #default="{ row }">{{ profileName(row) }}</template>
      </el-table-column>
      <el-table-column :label="t('settings.event.overview.colDevice')" min-width="120">
        <template #default="{ row }">{{ deviceName(row) }}</template>
      </el-table-column>
      <el-table-column
        :label="t('settings.event.overview.colAlarmCount')"
        prop="alarmCount"
        width="100"
        align="right"
      />
      <el-table-column
        :label="t('settings.event.overview.colPeerMedian')"
        prop="peerMedian"
        width="100"
        align="right"
      />
      <el-table-column :label="t('settings.event.overview.colRatio')" width="100" align="right">
        <template #default="{ row }">
          <el-tag :type="row.ratio >= 5 ? 'danger' : 'warning'" size="small">
            {{ row.ratio ? `${row.ratio}×` : '—' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import { onMounted, reactive, ref, watch } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';

  import { alertPeerDeviation } from '@/api/dashboard';
  import type { PeerDeviation } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getProfileByIds } from '@/api/profile';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';

  const { t } = useI18n();
  const router = useRouter();

  const daysOptions = [
    { label: '1d', value: '1' },
    { label: '7d', value: '7' },
    { label: '30d', value: '30' },
  ];
  const daysKey = ref<string>('7');

  const loading = ref(false);
  const rows = ref<PeerDeviation[]>([]);
  const nameMap = reactive<{ devices: Record<string, string>; profiles: Record<string, string> }>({
    devices: {},
    profiles: {},
  });

  const load = async () => {
    loading.value = true;
    try {
      const res: { data?: PeerDeviation[] } = await alertPeerDeviation(Number(daysKey.value));
      rows.value = res?.data ?? [];
      await resolveNames(rows.value);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  watch(daysKey, load);
  onMounted(load);

  const resolveNames = async (batch: PeerDeviation[]) => {
    const devIds = Array.from(new Set(batch.map((r) => String(r.deviceId)))).filter((id) => !nameMap.devices[id]);
    const profIds = Array.from(new Set(batch.map((r) => String(r.profileId)))).filter((id) => !nameMap.profiles[id]);
    const jobs: Promise<void>[] = [];
    if (devIds.length) {
      jobs.push(
        getDeviceByIds(devIds)
          .then((r: { data?: Record<string, { deviceName?: string }> }) => {
            const d = r?.data || {};
            for (const id of devIds) if (d[id]) nameMap.devices[id] = d[id].deviceName || id;
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

  const deviceName = (r: PeerDeviation) => nameMap.devices[String(r.deviceId)] || String(r.deviceId);
  const profileName = (r: PeerDeviation) => nameMap.profiles[String(r.profileId)] || String(r.profileId);

  const onRowClick = (row: PeerDeviation) => {
    router.push({ name: 'settingsDeviceEvent', query: { sourceId: String(row.deviceId) } }).catch(() => {});
  };
</script>

<style lang="scss" scoped>
  .peer-deviation {
    :deep(.el-table__row) {
      cursor: pointer;
    }
  }
</style>
