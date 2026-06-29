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
    :empty-text="t('settings.event.overview.protocolEmpty')"
    :loading="loading"
    :title="t('settings.event.overview.protocolTitle')"
    body-mode="scroll"
    class="protocol-health"
    loading-target="button"
    @refresh="load"
  >
    <el-table :data="rows" size="small">
      <el-table-column :label="t('settings.event.overview.colProtocol')" min-width="140">
        <template #default="{row}">
          <span class="protocol-health__name">{{ stripPrefix(row.serviceName) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('settings.event.overview.colDrivers')" align="right" prop="driverCount" width="100" />
      <!-- @vue-generic {ProtocolHealth} -->
      <el-table-column :label="t('settings.event.overview.colEnabled')" align="right" width="120">
        <template #default="{row}">
          <span :class="enabledClass(row)" class="protocol-health__enabled">
            {{ row.enabledCount }} / {{ row.driverCount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="t('settings.event.overview.colDevices')" align="right" prop="deviceCount" width="100" />
      <!-- @vue-generic {ProtocolHealth} -->
      <el-table-column :label="t('settings.event.overview.colHealthRatio')" min-width="130">
        <template #default="{row}">
          <el-progress
            :color="healthColor(row)"
            :percentage="healthPercent(row)"
            :show-text="false"
            :stroke-width="8"
          />
        </template>
      </el-table-column>
    </el-table>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import {onMounted, ref} from 'vue';
  import {useI18n} from 'vue-i18n';

  import {protocolHealth} from '@/api/dashboard';
  import type {ProtocolHealth} from '@/config/types/dashboard';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
  import {useAsyncLoader} from '@/utils/asyncLoaderUtil';

  const {t} = useI18n();
  const {loading, run} = useAsyncLoader();

  const rows = ref<ProtocolHealth[]>([]);

  const load = () =>
    run(async () => {
      const res: {data?: ProtocolHealth[]} = await protocolHealth();
      rows.value = res?.data ?? [];
    });

  // Backend keeps the raw `dc3-driver-*` service name; strip the prefix
  // here so the table reads "modbus-tcp / mqtt / opcua" — consistent with
  // how AnalyticsTabs.protocol renders it on Home.
  const stripPrefix = (s: string) => (s || '').replace(/^dc3-driver-/, '');

  const healthPercent = (r: ProtocolHealth) =>
    r.driverCount === 0 ? 0 : Math.round((r.enabledCount / r.driverCount) * 100);

  const healthColor = (r: ProtocolHealth) => {
    const p = healthPercent(r);
    if (p === 100) return '#67c23a';
    if (p >= 50) return '#e6a23c';
    return '#f56c6c';
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
      color: #303133;
      font-size: 13px;
    }

    .protocol-health__enabled {
      font-weight: 600;

      &--ok {
        color: #67c23a;
      }

      &--warn {
        color: #e6a23c;
      }

      &--bad {
        color: #f56c6c;
      }
    }
  }
</style>
