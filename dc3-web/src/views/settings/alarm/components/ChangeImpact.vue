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
    :empty="!loading && rows.length === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.changeImpactEmpty')"
    :loading="loading"
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
        <div class="change-impact__row" @click="onJump(row)">
          <el-tag :type="tagTypeFor(row.kind)" size="small">{{ kindLabel(row.kind) }}</el-tag>
          <span class="change-impact__name">{{ entityName(row) }}</span>
        </div>
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

const {t} = useI18n();
const router = useRouter();
const {loading, run} = useAsyncLoader();
const {resolveDevices, resolveDrivers, resolveProfiles, deviceName, driverName, profileName} = useEntityNames();

const daysOptions = [
  {label: '1d', value: '1'},
  {label: '7d', value: '7'},
  {label: '30d', value: '30'},
];
const daysKey = ref<string>('7');

const rows = ref<ChangeImpact[]>([]);

const load = () =>
  run(async () => {
    const res: { data?: ChangeImpact[] } = await alertChangeImpact(Number(daysKey.value), 30);
    rows.value = res?.data ?? [];
    // Each entityId goes through its kind-specific batch endpoint.
    await Promise.all([
      resolveDrivers(rows.value.filter((r) => r.kind === 'driver').map((r) => r.entityId)),
      resolveDevices(rows.value.filter((r) => r.kind === 'device').map((r) => r.entityId)),
      resolveProfiles(rows.value.filter((r) => r.kind === 'profile').map((r) => r.entityId)),
    ]);
  });

watch(daysKey, load);
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
