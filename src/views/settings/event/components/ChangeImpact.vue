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
    class="change-impact"
    :title="t('settings.event.overview.changeImpactTitle')"
    :subtitle="t('settings.event.overview.changeImpactSubtitle', { days: Number(daysKey) })"
    :loading="loading"
    loading-target="button"
    :empty="!loading && rows.length === 0"
    :empty-text="t('settings.event.overview.changeImpactEmpty')"
    :empty-image-size="60"
    body-mode="scroll"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="daysKey" :options="daysOptions" size="small" />
    </template>

    <el-timeline class="change-impact__timeline">
      <el-timeline-item
        v-for="row in rows"
        :key="`${row.kind}:${row.entityId}:${row.operateTime}`"
        :timestamp="formatTime(row.operateTime)"
        :color="colourFor(row.kind)"
        placement="top"
      >
        <div class="change-impact__row" @click="onJump(row)">
          <el-tag :type="tagTypeFor(row.kind)" size="small">{{ kindLabel(row.kind) }}</el-tag>
          <span class="change-impact__name">{{ entityName(row) }}</span>
        </div>
      </el-timeline-item>
    </el-timeline>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import { onMounted, reactive, ref, watch } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useRouter } from 'vue-router';

  import { alertChangeImpact } from '@/api/dashboard';
  import type { ChangeImpact } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getDriverByIds } from '@/api/driver';
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
  const rows = ref<ChangeImpact[]>([]);
  const nameMap = reactive<{
    driver: Record<string, string>;
    device: Record<string, string>;
    profile: Record<string, string>;
  }>({ driver: {}, device: {}, profile: {} });

  const load = async () => {
    loading.value = true;
    try {
      const res: { data?: ChangeImpact[] } = await alertChangeImpact(Number(daysKey.value), 30);
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

  const resolveNames = async (batch: ChangeImpact[]) => {
    const drvIds = new Set<string>();
    const devIds = new Set<string>();
    const profIds = new Set<string>();
    for (const r of batch) {
      if (r.kind === 'driver') drvIds.add(String(r.entityId));
      else if (r.kind === 'device') devIds.add(String(r.entityId));
      else if (r.kind === 'profile') profIds.add(String(r.entityId));
    }
    const drvMiss = [...drvIds].filter((id) => !nameMap.driver[id]);
    const devMiss = [...devIds].filter((id) => !nameMap.device[id]);
    const profMiss = [...profIds].filter((id) => !nameMap.profile[id]);
    const jobs: Promise<void>[] = [];
    if (drvMiss.length)
      jobs.push(
        getDriverByIds(drvMiss)
          .then((r: { data?: Record<string, { driverName?: string }> }) => {
            const d = r?.data || {};
            for (const id of drvMiss) if (d[id]) nameMap.driver[id] = d[id].driverName || id;
          })
          .catch(() => {})
      );
    if (devMiss.length)
      jobs.push(
        getDeviceByIds(devMiss)
          .then((r: { data?: Record<string, { deviceName?: string }> }) => {
            const d = r?.data || {};
            for (const id of devMiss) if (d[id]) nameMap.device[id] = d[id].deviceName || id;
          })
          .catch(() => {})
      );
    if (profMiss.length)
      jobs.push(
        getProfileByIds(profMiss)
          .then((r: { data?: Record<string, { profileName?: string }> }) => {
            const d = r?.data || {};
            for (const id of profMiss) if (d[id]) nameMap.profile[id] = d[id].profileName || id;
          })
          .catch(() => {})
      );
    await Promise.all(jobs);
  };

  const entityName = (r: ChangeImpact) => {
    const id = String(r.entityId);
    if (r.kind === 'driver') return nameMap.driver[id] || id;
    if (r.kind === 'device') return nameMap.device[id] || id;
    return nameMap.profile[id] || id;
  };

  const kindLabel = (k: string) => {
    if (k === 'driver') return t('settings.event.overview.kindDriver');
    if (k === 'device') return t('settings.event.overview.kindDevice');
    return t('settings.event.overview.kindProfile');
  };

  const colourFor = (k: string) => {
    if (k === 'driver') return '#9059f6';
    if (k === 'device') return '#409eff';
    return '#e6a23c';
  };

  const tagTypeFor = (k: string): 'primary' | 'warning' | 'success' | 'info' => {
    if (k === 'driver') return 'info';
    if (k === 'device') return 'primary';
    return 'warning';
  };

  const formatTime = (v: string) => {
    if (!v) return '';
    const d = new Date(v);
    if (Number.isNaN(d.getTime())) return v;
    return d.toLocaleString('zh-CN', { hour12: false });
  };

  const onJump = (r: ChangeImpact) => {
    const id = String(r.entityId);
    if (r.kind === 'driver') router.push({ name: 'driverDetail', query: { id, active: 'detail' } }).catch(() => {});
    else if (r.kind === 'device')
      router.push({ name: 'deviceDetail', query: { id, active: 'detail' } }).catch(() => {});
    else router.push({ name: 'profileDetail', query: { id, active: 'detail' } }).catch(() => {});
  };
</script>

<style lang="scss" scoped>
  .change-impact {
    .change-impact__timeline {
      padding: 12px 16px 0;
    }
    .change-impact__row {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      &:hover .change-impact__name {
        color: #409eff;
      }
    }
    .change-impact__name {
      font-size: 13px;
      color: #303133;
      font-weight: 500;
    }
  }
</style>
