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
    :empty-text="t('settings.event.overview.protocolEmpty')"
    :loading="loading"
    :error="status === 'error'"
    :error-text="t('common.loadFailed')"
    :retry-text="t('common.retry')"
    :title="t('settings.event.overview.protocolTitle')"
    body-mode="scroll"
    class="protocol-health"
    loading-target="button"
    @refresh="load"
  >
    <responsive-record-list
      :columns="columns"
      :loading="loading"
      :rows="rows"
      :status="status"
      embedded
      row-key="serviceName"
      @retry="load"
    >
      <template #cell-serviceName="{row}">
        <span class="protocol-health__name">{{ stripPrefix(row.serviceName) }}</span>
      </template>
      <template #cell-enabled="{row}">
        <span :class="enabledClass(row)" class="protocol-health__enabled">
          {{ row.enabledCount }} / {{ row.driverCount }}
        </span>
      </template>
      <template #cell-health="{row}">
        <el-progress
          :color="healthColor(row)"
          :percentage="healthPercent(row)"
          :show-text="false"
          :stroke-width="8"
        />
      </template>
    </responsive-record-list>
  </dashboard-card>
</template>

<script lang="ts" setup>
import {computed, onMounted, ref} from 'vue';
import {useI18n} from 'vue-i18n';

import {protocolHealth} from '@/api/dashboard';
import type {ProtocolHealth} from '@/config/types/dashboard';
import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import {useAsyncLoader} from '@/utils/asyncLoaderUtil';

const {t} = useI18n();
const {loading, run, status} = useAsyncLoader();

const rows = ref<ProtocolHealth[]>([]);
const columns = computed(() => [
  {
    key: 'serviceName',
    label: t('settings.event.overview.colProtocol'),
    minWidth: 140,
    kind: 'custom' as const,
    mobile: 'primary' as const,
  },
  {key: 'driverCount', label: t('settings.event.overview.colDrivers'), width: 100, mobile: 'detail' as const},
  {
    key: 'enabled',
    label: t('settings.event.overview.colEnabled'),
    width: 120,
    kind: 'custom' as const,
    mobile: 'detail' as const,
  },
  {key: 'deviceCount', label: t('settings.event.overview.colDevices'), width: 100, mobile: 'detail' as const},
  {
    key: 'health',
    label: t('settings.event.overview.colHealthRatio'),
    minWidth: 130,
    kind: 'custom' as const,
    mobile: 'detail' as const,
  },
]);

const load = () =>
  run(() => protocolHealth() as Promise<ProtocolHealth[]>, {
    apply: (result) => (rows.value = result ?? []),
  });

// Backend keeps the raw `dc3-driver-*` service name; strip the prefix
// here so the table reads "modbus-tcp / mqtt / opcua" — consistent with
// how AnalyticsTabs.protocol renders it on Home.
const stripPrefix = (s: string) => (s || '').replace(/^dc3-driver-/, '');

const healthPercent = (r: ProtocolHealth) =>
  r.driverCount === 0 ? 0 : Math.round((r.enabledCount / r.driverCount) * 100);

const healthColor = (r: ProtocolHealth) => {
  const p = healthPercent(r);
  if (p === 100) return 'var(--el-color-success)';
  if (p >= 50) return 'var(--el-color-warning)';
  return 'var(--el-color-danger)';
};

const enabledClass = (r: ProtocolHealth) => {
  const p = healthPercent(r);
  return {
    'protocol-health__enabled--ok': p === 100,
    'protocol-health__enabled--warn': p >= 50 && p < 100,
    'protocol-health__enabled--bad': p < 50,
  };
};

onMounted(load);

defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
.protocol-health {
  .protocol-health__name {
    font-family: 'Menlo', monospace;
    color: var(--dc3-text-primary);
    font-size: 13px;
  }

  .protocol-health__enabled {
    font-weight: 600;

    &--ok {
      color: var(--el-color-success);
    }

    &--warn {
      color: var(--el-color-warning);
    }

    &--bad {
      color: var(--el-color-danger);
    }
  }
}
</style>
